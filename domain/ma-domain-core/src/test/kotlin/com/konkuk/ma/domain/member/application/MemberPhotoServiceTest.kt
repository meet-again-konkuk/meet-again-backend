package com.konkuk.ma.domain.member.application

import com.konkuk.ma.domain.common.domain.file.port.FileStorage
import com.konkuk.ma.domain.common.domain.file.port.ThumbnailGenerator
import com.konkuk.ma.domain.common.fixture.PhotoFileFixture
import com.konkuk.ma.domain.member.domain.photo.NewPhoto
import com.konkuk.ma.domain.member.domain.port.MemberPhotoRepository
import com.konkuk.ma.domain.member.fixture.MemberPhotoFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class MemberPhotoServiceTest : FunSpec({

    val fileStorage = mockk<FileStorage>()
    val thumbnailGenerator = mockk<ThumbnailGenerator>()
    val memberPhotoRepository = mockk<MemberPhotoRepository>()
    val service = MemberPhotoService(fileStorage, thumbnailGenerator, memberPhotoRepository)

    beforeEach {
        clearAllMocks()
    }

    context("upload") {

        test("새 사진을 업로드하면 썸네일도 함께 저장된다") {
            // Given
            val email = "user@example.com"
            val photoFile = PhotoFileFixture.create()
            val storedFilePath = "member/profile/$email/${photoFile.originalFileName}"
            val thumbnailBytes = ByteArray(512) { 2 }
            val storedThumbnailPath = "member/thumbnail/$email/thumb_${photoFile.originalFileName}"

            every { memberPhotoRepository.findByMemberEmail(email) } returns null
            every { fileStorage.store(any(), photoFile) } returns storedFilePath
            every { thumbnailGenerator.generate(photoFile.content, 400) } returns thumbnailBytes
            every { fileStorage.storeBytes(any(), "thumb_${photoFile.originalFileName}", thumbnailBytes) } returns storedThumbnailPath
            val capturedNewPhoto = slot<NewPhoto>()
            every { memberPhotoRepository.save(capture(capturedNewPhoto)) } returns 1L

            // When
            service.upload(email, photoFile)

            // Then
            capturedNewPhoto.captured.thumbnailPath shouldBe storedThumbnailPath
            capturedNewPhoto.captured.filePath shouldBe storedFilePath
            capturedNewPhoto.captured.memberEmail shouldBe email
            capturedNewPhoto.captured.originalFileName shouldBe photoFile.originalFileName
        }

        test("썸네일 생성에 실패해도 사진 업로드는 성공한다") {
            // Given
            val email = "user@example.com"
            val photoFile = PhotoFileFixture.create()
            val storedFilePath = "member/profile/$email/${photoFile.originalFileName}"

            every { memberPhotoRepository.findByMemberEmail(email) } returns null
            every { fileStorage.store(any(), photoFile) } returns storedFilePath
            every { thumbnailGenerator.generate(photoFile.content, 400) } throws RuntimeException("이미지 처리 실패")
            val capturedNewPhoto = slot<NewPhoto>()
            every { memberPhotoRepository.save(capture(capturedNewPhoto)) } returns 1L

            // When
            service.upload(email, photoFile)

            // Then
            capturedNewPhoto.captured.thumbnailPath shouldBe null
            capturedNewPhoto.captured.filePath shouldBe storedFilePath
        }

        test("기존 사진이 있으면 삭제 후 새 사진을 업로드한다") {
            // Given
            val email = "user@example.com"
            val photoFile = PhotoFileFixture.create()
            val existingPhoto = MemberPhotoFixture.create(
                memberEmail = email,
                thumbnailPath = "member/thumbnail/$email/thumb_old.jpg"
            )

            every { memberPhotoRepository.findByMemberEmail(email) } returns existingPhoto
            every { fileStorage.delete(existingPhoto.filePath) } returns Unit
            every { fileStorage.delete(existingPhoto.thumbnailPath!!) } returns Unit
            every { memberPhotoRepository.deleteByMemberEmail(email) } returns Unit
            every { fileStorage.store(any(), photoFile) } returns "new/path.jpg"
            every { thumbnailGenerator.generate(photoFile.content, 400) } returns ByteArray(100)
            every { fileStorage.storeBytes(any(), any(), any()) } returns "new/thumb/path.jpg"
            every { memberPhotoRepository.save(any()) } returns 2L

            // When
            service.upload(email, photoFile)

            // Then
            verify { fileStorage.delete(existingPhoto.filePath) }
            verify { fileStorage.delete(existingPhoto.thumbnailPath!!) }
            verify { memberPhotoRepository.deleteByMemberEmail(email) }
            verify { memberPhotoRepository.save(any()) }
        }
    }

    context("delete") {

        test("기존 사진이 있으면 파일과 DB 레코드를 삭제한다") {
            // Given
            val email = "user@example.com"
            val existingPhoto = MemberPhotoFixture.create(memberEmail = email)

            every { memberPhotoRepository.findByMemberEmail(email) } returns existingPhoto
            every { fileStorage.delete(existingPhoto.filePath) } returns Unit
            every { memberPhotoRepository.deleteByMemberEmail(email) } returns Unit

            // When
            service.delete(email)

            // Then
            verify { fileStorage.delete(existingPhoto.filePath) }
            verify { memberPhotoRepository.deleteByMemberEmail(email) }
        }

        test("썸네일이 있는 사진을 삭제하면 썸네일 파일도 삭제한다") {
            // Given
            val email = "user@example.com"
            val thumbnailPath = "member/thumbnail/$email/thumb_photo.jpg"
            val existingPhoto = MemberPhotoFixture.create(
                memberEmail = email,
                thumbnailPath = thumbnailPath
            )

            every { memberPhotoRepository.findByMemberEmail(email) } returns existingPhoto
            every { fileStorage.delete(existingPhoto.filePath) } returns Unit
            every { fileStorage.delete(thumbnailPath) } returns Unit
            every { memberPhotoRepository.deleteByMemberEmail(email) } returns Unit

            // When
            service.delete(email)

            // Then
            verify { fileStorage.delete(thumbnailPath) }
        }

        test("썸네일이 없는 사진을 삭제하면 원본 파일만 삭제한다") {
            // Given
            val email = "user@example.com"
            val existingPhoto = MemberPhotoFixture.create(
                memberEmail = email,
                thumbnailPath = null
            )

            every { memberPhotoRepository.findByMemberEmail(email) } returns existingPhoto
            every { fileStorage.delete(existingPhoto.filePath) } returns Unit
            every { memberPhotoRepository.deleteByMemberEmail(email) } returns Unit

            // When
            service.delete(email)

            // Then
            verify(exactly = 1) { fileStorage.delete(existingPhoto.filePath) }
            verify { memberPhotoRepository.deleteByMemberEmail(email) }
        }

        test("기존 사진이 없으면 아무 동작도 하지 않는다") {
            // Given
            val email = "nonexistent@example.com"

            every { memberPhotoRepository.findByMemberEmail(email) } returns null

            // When
            service.delete(email)

            // Then
            verify(exactly = 0) { fileStorage.delete(any()) }
            verify(exactly = 0) { memberPhotoRepository.deleteByMemberEmail(any()) }
        }
    }
})
