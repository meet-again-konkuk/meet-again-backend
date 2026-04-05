package com.konkuk.ma.domain.member.application

import com.konkuk.ma.domain.common.fixture.PhotoFileFixture
import com.konkuk.ma.domain.member.domain.photo.MemberPhotoProcessor
import com.konkuk.ma.domain.member.domain.photo.NewPhoto
import com.konkuk.ma.domain.member.domain.photo.ProcessedPhoto
import com.konkuk.ma.domain.member.domain.port.MemberPhotoRepository
import com.konkuk.ma.domain.member.fixture.MemberPhotoFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify

class MemberPhotoServiceTest : FunSpec({

    val memberPhotoProcessor = mockk<MemberPhotoProcessor>()
    val memberPhotoRepository = mockk<MemberPhotoRepository>()
    val service = MemberPhotoService(memberPhotoProcessor, memberPhotoRepository)

    beforeEach {
        clearAllMocks()
    }

    context("upload") {

        test("새 사진을 업로드하면 processor로 처리하고 DB에 저장한다") {
            // Given
            val email = "user@example.com"
            val photoFile = PhotoFileFixture.create()
            val processed = ProcessedPhoto("stored/path.jpg", "stored/thumb.jpg")

            every { memberPhotoRepository.findByMemberEmail(email) } returns null
            every { memberPhotoProcessor.process(email, photoFile) } returns processed
            val capturedNewPhoto = slot<NewPhoto>()
            every { memberPhotoRepository.save(capture(capturedNewPhoto)) } returns 1L

            // When
            service.upload(email, photoFile)

            // Then
            capturedNewPhoto.captured.filePath shouldBe processed.filePath
            capturedNewPhoto.captured.thumbnailPath shouldBe processed.thumbnailPath
            capturedNewPhoto.captured.memberEmail shouldBe email
        }

        test("기존 사진이 있으면 삭제 후 새 사진을 업로드한다") {
            // Given
            val email = "user@example.com"
            val photoFile = PhotoFileFixture.create()
            val existingPhoto = MemberPhotoFixture.create(
                memberEmail = email,
                thumbnailPath = "old/thumb.jpg"
            )
            val processed = ProcessedPhoto("new/path.jpg", "new/thumb.jpg")

            every { memberPhotoRepository.findByMemberEmail(email) } returns existingPhoto
            every { memberPhotoProcessor.deleteFile(existingPhoto.filePath) } just runs
            every { memberPhotoProcessor.deleteFile(existingPhoto.thumbnailPath!!) } just runs
            every { memberPhotoRepository.deleteByMemberEmail(email) } just runs
            every { memberPhotoProcessor.process(email, photoFile) } returns processed
            every { memberPhotoRepository.save(any()) } returns 2L

            // When
            service.upload(email, photoFile)

            // Then
            verify { memberPhotoProcessor.deleteFile(existingPhoto.filePath) }
            verify { memberPhotoProcessor.deleteFile(existingPhoto.thumbnailPath!!) }
            verify { memberPhotoRepository.deleteByMemberEmail(email) }
            verify { memberPhotoProcessor.process(email, photoFile) }
            verify { memberPhotoRepository.save(any()) }
        }
    }

    context("delete") {

        test("기존 사진이 있으면 파일과 DB 레코드를 삭제한다") {
            // Given
            val email = "user@example.com"
            val existingPhoto = MemberPhotoFixture.create(memberEmail = email)

            every { memberPhotoRepository.findByMemberEmail(email) } returns existingPhoto
            every { memberPhotoProcessor.deleteFile(existingPhoto.filePath) } just runs
            every { memberPhotoRepository.deleteByMemberEmail(email) } just runs

            // When
            service.delete(email)

            // Then
            verify { memberPhotoProcessor.deleteFile(existingPhoto.filePath) }
            verify { memberPhotoRepository.deleteByMemberEmail(email) }
        }

        test("썸네일이 있는 사진을 삭제하면 썸네일도 삭제한다") {
            // Given
            val email = "user@example.com"
            val existingPhoto = MemberPhotoFixture.create(
                memberEmail = email,
                thumbnailPath = "member/thumbnail/thumb.jpg"
            )

            every { memberPhotoRepository.findByMemberEmail(email) } returns existingPhoto
            every { memberPhotoProcessor.deleteFile(existingPhoto.filePath) } just runs
            every { memberPhotoProcessor.deleteFile(existingPhoto.thumbnailPath!!) } just runs
            every { memberPhotoRepository.deleteByMemberEmail(email) } just runs

            // When
            service.delete(email)

            // Then
            verify { memberPhotoProcessor.deleteFile(existingPhoto.thumbnailPath!!) }
        }

        test("썸네일이 없는 사진을 삭제하면 원본만 삭제한다") {
            // Given
            val email = "user@example.com"
            val existingPhoto = MemberPhotoFixture.create(
                memberEmail = email,
                thumbnailPath = null
            )

            every { memberPhotoRepository.findByMemberEmail(email) } returns existingPhoto
            every { memberPhotoProcessor.deleteFile(existingPhoto.filePath) } just runs
            every { memberPhotoRepository.deleteByMemberEmail(email) } just runs

            // When
            service.delete(email)

            // Then
            verify(exactly = 1) { memberPhotoProcessor.deleteFile(existingPhoto.filePath) }
        }

        test("기존 사진이 없으면 아무 동작도 하지 않는다") {
            // Given
            val email = "nonexistent@example.com"

            every { memberPhotoRepository.findByMemberEmail(email) } returns null

            // When
            service.delete(email)

            // Then
            verify(exactly = 0) { memberPhotoProcessor.deleteFile(any()) }
            verify(exactly = 0) { memberPhotoRepository.deleteByMemberEmail(any()) }
        }
    }
})
