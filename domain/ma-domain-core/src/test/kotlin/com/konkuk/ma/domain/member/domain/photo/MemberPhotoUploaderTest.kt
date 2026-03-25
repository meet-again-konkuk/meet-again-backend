package com.konkuk.ma.domain.member.domain.photo

import com.konkuk.ma.domain.common.domain.file.StorageDomainType
import com.konkuk.ma.domain.common.domain.file.StoragePath
import com.konkuk.ma.domain.common.domain.file.StorageUsageType
import com.konkuk.ma.domain.common.domain.file.port.FileStorage
import com.konkuk.ma.domain.common.fixture.PhotoFileFixture
import com.konkuk.ma.domain.member.domain.port.MemberPhotoRepository
import com.konkuk.ma.domain.member.fixture.MemberPhotoFixture
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify

class MemberPhotoUploaderTest : FunSpec({

    val fileStorage = mockk<FileStorage>()
    val memberPhotoRepository = mockk<MemberPhotoRepository>()
    val uploader = MemberPhotoUploader(fileStorage, memberPhotoRepository)

    val email = "test@example.com"
    val expectedDirectory = StoragePath.of(StorageDomainType.MEMBER, StorageUsageType.PROFILE, email).value

    beforeEach {
        clearAllMocks()
    }

    context("upload") {
        test("photoFile이 null이면 아무 동작도 하지 않는다") {
            // When
            uploader.upload(email, null)

            // Then
            verify(exactly = 0) { fileStorage.store(any(), any()) }
            verify(exactly = 0) { memberPhotoRepository.save(any()) }
        }

        test("기존 사진이 없으면 새 사진을 업로드한다") {
            // Given
            val photoFile = PhotoFileFixture.create()
            val storedFilePath = "$expectedDirectory/stored-photo.jpg"

            every { memberPhotoRepository.findByMemberEmail(email) } returns null
            every { fileStorage.store(expectedDirectory, photoFile) } returns storedFilePath
            every { memberPhotoRepository.save(any()) } returns 1L

            // When
            uploader.upload(email, photoFile)

            // Then
            verify(exactly = 1) { fileStorage.store(expectedDirectory, photoFile) }
            verify(exactly = 1) { memberPhotoRepository.save(any()) }
            verify(exactly = 0) { fileStorage.delete(any()) }
            verify(exactly = 0) { memberPhotoRepository.deleteByMemberEmail(any()) }
        }

        test("기존 사진이 있으면 삭제 후 새 사진을 업로드한다") {
            // Given
            val photoFile = PhotoFileFixture.create()
            val existingPhoto = MemberPhotoFixture.create(memberEmail = email)
            val storedFilePath = "$expectedDirectory/stored-photo.jpg"

            every { memberPhotoRepository.findByMemberEmail(email) } returns existingPhoto
            every { fileStorage.delete(existingPhoto.filePath) } just runs
            every { memberPhotoRepository.deleteByMemberEmail(email) } just runs
            every { fileStorage.store(expectedDirectory, photoFile) } returns storedFilePath
            every { memberPhotoRepository.save(any()) } returns 2L

            // When
            uploader.upload(email, photoFile)

            // Then
            verify(exactly = 1) { fileStorage.delete(existingPhoto.filePath) }
            verify(exactly = 1) { memberPhotoRepository.deleteByMemberEmail(email) }
            verify(exactly = 1) { fileStorage.store(expectedDirectory, photoFile) }
            verify(exactly = 1) { memberPhotoRepository.save(any()) }
        }
    }
})
