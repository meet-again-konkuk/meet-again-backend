package com.konkuk.ma.domain.member.domain.photo

import com.konkuk.ma.domain.common.domain.Email
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

        test("원본과 썸네일을 저장하고 ProcessedPhoto를 반환한다") {
            // Given
            val email = Email("user@example.com")
            val photoFile = PhotoFileFixture.create()

            every { fileStorage.store(any(), photoFile) } returns "member/profile/user@example.com/photo.jpg"
            every { thumbnailGenerator.generate(photoFile.content, 400) } returns ByteArray(512)
            every { fileStorage.storeBytes(any(), "thumb_${photoFile.originalFileName}", any()) } returns "member/thumbnail/user@example.com/thumb_photo.jpg"

            // When
            val result = processor.process(email, photoFile)

            // Then
            result.filePath shouldBe "member/profile/user@example.com/photo.jpg"
            result.thumbnailPath shouldBe "member/thumbnail/user@example.com/thumb_photo.jpg"
        }

        test("썸네일 생성에 실패하면 thumbnailPath가 null인 ProcessedPhoto를 반환한다") {
            // Given
            val email = Email("user@example.com")
            val photoFile = PhotoFileFixture.create()

            every { fileStorage.store(any(), photoFile) } returns "member/profile/user@example.com/photo.jpg"
            every { thumbnailGenerator.generate(photoFile.content, 400) } throws RuntimeException("생성 실패")

            // When
            val result = processor.process(email, photoFile)

            // Then
            result.filePath shouldNotBe null
            result.thumbnailPath shouldBe null
        }
    }

    context("deleteFiles") {

        test("썸네일이 있는 사진은 원본과 썸네일 모두 삭제한다") {
            // Given
            val photo = MemberPhotoFixture.create(
                filePath = "member/profile/photo.jpg",
                thumbnailPath = "member/thumbnail/thumb_photo.jpg"
            )

            every { fileStorage.delete(any()) } just runs

            // When
            processor.deleteFiles(photo)

            // Then
            verify { fileStorage.delete(photo.filePath) }
            verify { fileStorage.delete(photo.thumbnailPath!!) }
        }

        test("썸네일이 없는 사진은 원본만 삭제한다") {
            // Given
            val photo = MemberPhotoFixture.create(
                filePath = "member/profile/photo.jpg",
                thumbnailPath = null
            )

            every { fileStorage.delete(any()) } just runs

            // When
            processor.deleteFiles(photo)

            // Then
            verify(exactly = 1) { fileStorage.delete(photo.filePath) }
        }
    }
})
