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

/**
 * 게시글 이미지 처리기(PostImageProcessor) 계약 테스트.
 *
 * MediaProcessor 선례를 미러하되, community 규칙(결정 D2·D3)을 추가로 검증한다.
 * - 원본은 community/post-image/{postId}/ 상대 storageKey 로 저장한다.
 * - 썸네일은 best-effort — 실패해도 원본은 저장하고 thumbnailKey 만 null 이 된다(결정 D4).
 * - 확장자 화이트리스트(jpg/jpeg/png/webp, svg 배제) + 매직바이트 일치 검증 실패 시 InvalidValueException.
 */
class PostImageProcessorTest : FunSpec({

    val fileStorage = mockk<FileStorage>()
    val thumbnailGenerator = mockk<ThumbnailGenerator>()
    val processor = PostImageProcessor(fileStorage, thumbnailGenerator)

    beforeEach {
        clearAllMocks()
    }

    // 정상 서명으로 시작하는 내용물 바이트를 만든다 (매직바이트 검증 통과용).
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
            // Given - jpg 확장자 + JPEG 서명
            val postId = 7L
            val photo = photoFile("photo.jpg", 0xFF, 0xD8, 0xFF)
            val storedLeaf = "9f1c-uuid.jpg"
            every {
                fileStorage.store(any(), photo)
            } returns "/var/data/uploads/community/post-image/$postId/$storedLeaf"
            every { thumbnailGenerator.generate(photo.content, 400) } returns ByteArray(512)
            every { fileStorage.storeBytes(any(), any(), any()) } returns "/var/data/uploads/community/thumbnail/$postId/thumb"

            // When
            val result = processor.process(postId, photo)

            // Then - 절대경로 prefix 는 제거되고 directory + leaf 형태의 상대키가 되며 메타가 담긴다
            result.storageKey shouldBe "community/post-image/$postId/$storedLeaf"
            result.postId shouldBe postId
            result.originalFilename shouldBe photo.originalFileName
            result.mimeType shouldBe photo.mimeType
            result.fileSize shouldBe photo.sizeInBytes
        }

        test("썸네일 생성에 성공하면 thumbnailKey 를 community 상대 경로로 반환한다") {
            // Given
            val postId = 1L
            val photo = photoFile("photo.png", 0x89, 0x50, 0x4E, 0x47)
            every { fileStorage.store(any(), photo) } returns "/var/data/uploads/community/post-image/$postId/uuid.png"
            every { thumbnailGenerator.generate(photo.content, 400) } returns ByteArray(512)
            every { fileStorage.storeBytes(any(), any(), any()) } returns "/var/data/uploads/community/thumbnail/$postId/thumb"

            // When
            val result = processor.process(postId, photo)

            // Then
            result.thumbnailKey shouldBe "community/thumbnail/$postId/thumb_${photo.originalFileName}"
        }

        test("썸네일 생성에 실패해도 원본은 저장되고 thumbnailKey 는 null 이 된다") {
            // Given
            val postId = 1L
            val photo = photoFile("photo.jpg", 0xFF, 0xD8, 0xFF)
            every { fileStorage.store(any(), photo) } returns "/var/data/uploads/community/post-image/$postId/uuid.jpg"
            every { thumbnailGenerator.generate(photo.content, 400) } throws RuntimeException("생성 실패")

            // When
            val result = processor.process(postId, photo)

            // Then - 원본은 저장되지만 썸네일은 best-effort 로 null 이 된다
            result.storageKey shouldNotBe null
            result.thumbnailKey.shouldBeNull()
        }
    }

    context("process - 검증 실패") {

        test("svg 확장자는 커뮤니티에서 배제되어 InvalidValueException 이 발생한다") {
            // Given - PhotoFile 생성은 통과(svg 도 AllowedExtension) 하지만 처리기가 거부한다
            val photo = photoFile("vector.svg", 0x3C, 0x73, 0x76, 0x67)

            // When & Then
            shouldThrow<InvalidValueException> {
                processor.process(1L, photo)
            }
        }

        test("확장자와 내용물 서명이 불일치하는 위장 파일이면 InvalidValueException 이 발생한다") {
            // Given - 확장자는 jpg 이지만 내용물은 PNG 서명
            val disguised = photoFile("fake.jpg", 0x89, 0x50, 0x4E, 0x47)

            // When & Then
            shouldThrow<InvalidValueException> {
                processor.process(1L, disguised)
            }
        }
    }
})
