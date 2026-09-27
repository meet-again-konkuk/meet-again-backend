package com.konkuk.ma.domain.member.domain.photo

import com.konkuk.ma.domain.common.domain.file.port.FileStorage
import com.konkuk.ma.domain.common.domain.file.port.ThumbnailGenerator
import com.konkuk.ma.domain.common.fixture.PhotoFileFixture
import com.konkuk.ma.domain.member.fixture.MemberPhotoFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify

class MemberPhotoProcessorTest : FunSpec({

    val fileStorage = mockk<FileStorage>()
    val thumbnailGenerator = mockk<ThumbnailGenerator>()
    val processor = MemberPhotoProcessor(fileStorage, thumbnailGenerator)

    beforeEach {
        clearAllMocks()
    }

    context("process") {

        test("store가 절대경로를 반환해도 leaf만 추출해 상대 storageKey로 반환한다") {
            // Given - LocalFileStorage.store는 basePath가 붙은 절대경로를 돌려준다
            val memberId = 1L
            val photoFile = PhotoFileFixture.create()
            val storedLeaf = "9f1c-uuid.jpg"

            every { fileStorage.store(any(), photoFile) } returns "/var/data/uploads/member/profile/$memberId/$storedLeaf"
            every { thumbnailGenerator.generate(photoFile.content, 400) } returns ByteArray(512)
            every { fileStorage.storeBytes(any(), "thumb_${photoFile.originalFileName}", any()) } returns "/var/data/uploads/member/thumbnail/$memberId/thumb_${photoFile.originalFileName}"

            // When
            val result = processor.process(memberId, photoFile)

            // Then - basePath prefix는 벗겨지고 directory + leaf 형태의 상대키만 남는다
            result.storageKey shouldBe "member/profile/$memberId/$storedLeaf"
            result.thumbnailKey shouldBe "member/thumbnail/$memberId/thumb_${photoFile.originalFileName}"
        }

        test("store가 저장하며 파일명을 바꿔도 바뀐 파일명이 storageKey에 담긴다") {
            // Given - LocalFileStorage는 원본 파일명 대신 UUID 파일명으로 저장한다
            val memberId = 7L
            val photoFile = PhotoFileFixture.create(originalFileName = "내사진.jpg")
            val renamedLeaf = "c0ffee-uuid.jpg"

            every { fileStorage.store(any(), photoFile) } returns "uploads/member/profile/$memberId/$renamedLeaf"
            every { thumbnailGenerator.generate(photoFile.content, 400) } returns ByteArray(512)
            every { fileStorage.storeBytes(any(), any(), any()) } returns "uploads/member/thumbnail/$memberId/thumb_내사진.jpg"

            // When
            val result = processor.process(memberId, photoFile)

            // Then
            result.storageKey shouldBe "member/profile/$memberId/$renamedLeaf"
        }

        test("썸네일 키는 원본 파일명 앞에 thumb_ 를 붙인 상대키다") {
            // Given
            val memberId = 3L
            val photoFile = PhotoFileFixture.create(originalFileName = "photo.png")

            every { fileStorage.store(any(), photoFile) } returns "uploads/member/profile/$memberId/uuid.png"
            every { thumbnailGenerator.generate(photoFile.content, 400) } returns ByteArray(512)
            every { fileStorage.storeBytes(any(), any(), any()) } returns "무시되는-반환값"

            // When
            val result = processor.process(memberId, photoFile)

            // Then - storeBytes 의 반환값이 아니라 directory + fileName 으로 조립된다
            result.thumbnailKey shouldBe "member/thumbnail/$memberId/thumb_${photoFile.originalFileName}"
        }

        test("썸네일 생성에 실패하면 thumbnailKey가 null인 ProcessedPhoto를 반환한다") {
            // Given
            val memberId = 1L
            val photoFile = PhotoFileFixture.create()

            every { fileStorage.store(any(), photoFile) } returns "uploads/member/profile/$memberId/photo.jpg"
            every { thumbnailGenerator.generate(photoFile.content, 400) } throws RuntimeException("생성 실패")

            // When
            val result = processor.process(memberId, photoFile)

            // Then - 원본은 저장되지만 썸네일은 best-effort로 null이 된다
            result.storageKey shouldNotBe null
            result.thumbnailKey shouldBe null
        }
    }

    context("deleteFiles") {

        test("썸네일이 있는 사진은 원본과 썸네일 키를 모두 삭제한다") {
            // Given
            val photo = MemberPhotoFixture.create(
                storageKey = "member/profile/1/photo.jpg",
                thumbnailKey = "member/thumbnail/1/thumb_photo.jpg"
            )

            every { fileStorage.deleteByKey(any()) } just runs

            // When
            processor.deleteFiles(photo)

            // Then - 원본·썸네일 모두 상대 storageKey 로 삭제한다
            verify(exactly = 1) { fileStorage.deleteByKey(photo.storageKey) }
            verify(exactly = 1) { fileStorage.deleteByKey(photo.thumbnailKey!!) }
        }

        test("썸네일이 없는 사진은 원본만 삭제한다") {
            // Given
            val photo = MemberPhotoFixture.create(
                storageKey = "member/profile/1/photo.jpg",
                thumbnailKey = null
            )

            every { fileStorage.deleteByKey(any()) } just runs

            // When
            processor.deleteFiles(photo)

            // Then
            verify(exactly = 1) { fileStorage.deleteByKey(photo.storageKey) }
            verify(exactly = 1) { fileStorage.deleteByKey(any()) }
        }
    }
})
