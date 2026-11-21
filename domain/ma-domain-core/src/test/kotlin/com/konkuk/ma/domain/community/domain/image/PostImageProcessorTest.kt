package com.konkuk.ma.domain.community.domain.image

import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.common.domain.file.port.FileStorage
import com.konkuk.ma.domain.common.domain.file.port.ThumbnailGenerator
import com.konkuk.ma.domain.common.fixture.PhotoFileFixture
import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk

class PostImageProcessorTest : FunSpec({

    val fileStorage = mockk<FileStorage>()
    val thumbnailGenerator = mockk<ThumbnailGenerator>()
    val processor = PostImageProcessor(fileStorage, thumbnailGenerator)

    beforeEach {
        clearAllMocks()
    }

    fun contentStartingWith(vararg signature: Int): ByteArray =
        (signature.map { it.toByte() } + List(32) { 0.toByte() }).toByteArray()

    fun photoFile(fileName: String, vararg signature: Int): PhotoFile {
        val content = contentStartingWith(*signature)
        return PhotoFileFixture.create(
            originalFileName = fileName,
            sizeInBytes = content.size.toLong(),
            content = content,
        )
    }

    context("process - 정상 이미지") {

        test("store 가 절대경로를 반환해도 leaf 만 추출해 community 상대 storageKey 로 반환한다") {
            val postId = 7L
            val photo = photoFile("photo.jpg", 0xFF, 0xD8, 0xFF)
            val storedLeaf = "9f1c-uuid.jpg"
            every {
                fileStorage.store(any(), photo)
            } returns "/var/data/uploads/community/post-image/$postId/$storedLeaf"
            every { thumbnailGenerator.generate(photo.content, 400) } returns ByteArray(512)
            every { fileStorage.storeBytes(any(), any(), any()) } returns "/var/data/uploads/community/thumbnail/$postId/thumb"

            val result = processor.process(postId, photo)

            result.storageKey shouldBe "community/post-image/$postId/$storedLeaf"
            result.postId shouldBe postId
            result.originalFilename shouldBe photo.originalFileName
            result.mimeType shouldBe photo.mimeType
            result.fileSize shouldBe photo.sizeInBytes
        }

        test("썸네일 생성에 성공하면 thumbnailKey 를 community 상대 경로로 반환한다") {
            val postId = 1L
            val photo = photoFile("photo.png", 0x89, 0x50, 0x4E, 0x47)
            every { fileStorage.store(any(), photo) } returns "/var/data/uploads/community/post-image/$postId/uuid.png"
            every { thumbnailGenerator.generate(photo.content, 400) } returns ByteArray(512)
            every { fileStorage.storeBytes(any(), any(), any()) } returns "/var/data/uploads/community/thumbnail/$postId/thumb"

            val result = processor.process(postId, photo)

            result.thumbnailKey shouldBe "community/thumbnail/$postId/thumb_${photo.originalFileName}"
        }

        test("썸네일 생성에 실패해도 원본은 저장되고 thumbnailKey 는 null 이 된다") {
            val postId = 1L
            val photo = photoFile("photo.jpg", 0xFF, 0xD8, 0xFF)
            every { fileStorage.store(any(), photo) } returns "/var/data/uploads/community/post-image/$postId/uuid.jpg"
            every { thumbnailGenerator.generate(photo.content, 400) } throws RuntimeException("생성 실패")

            val result = processor.process(postId, photo)

            result.storageKey shouldNotBe null
            result.thumbnailKey.shouldBeNull()
        }
    }

    context("process - 검증 실패") {

        test("svg 확장자는 커뮤니티에서 배제되어 InvalidValueException 이 발생한다") {
            val photo = photoFile("vector.svg", 0x3C, 0x73, 0x76, 0x67)

            shouldThrow<InvalidValueException> {
                processor.process(1L, photo)
            }
        }

        test("확장자와 내용물 서명이 불일치하는 위장 파일이면 InvalidValueException 이 발생한다") {
            val disguised = photoFile("fake.jpg", 0x89, 0x50, 0x4E, 0x47)

            shouldThrow<InvalidValueException> {
                processor.process(1L, disguised)
            }
        }
    }
})
