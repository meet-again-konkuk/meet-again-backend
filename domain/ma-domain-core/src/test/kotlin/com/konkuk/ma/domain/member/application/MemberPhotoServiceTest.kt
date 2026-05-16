package com.konkuk.ma.domain.member.application

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.common.fixture.PhotoFileFixture
import com.konkuk.ma.domain.member.domain.photo.MemberPhotoCleaner
import com.konkuk.ma.domain.member.domain.photo.MemberPhotoProcessor
import com.konkuk.ma.domain.member.domain.photo.NewPhoto
import com.konkuk.ma.domain.member.domain.photo.ProcessedPhoto
import com.konkuk.ma.domain.member.domain.port.MemberPhotoRepository
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
    val memberPhotoCleaner = mockk<MemberPhotoCleaner>()
    val service = MemberPhotoService(memberPhotoProcessor, memberPhotoRepository, memberPhotoCleaner)

    beforeEach {
        clearAllMocks()
    }

    context("upload") {

        test("새 사진을 업로드하면 cleaner로 정리 후 processor로 처리하고 DB에 저장한다") {
            // Given
            val email = "user@example.com"
            val photoFile = PhotoFileFixture.create()
            val processed = ProcessedPhoto("stored/path.jpg", "stored/thumb.jpg")

            every { memberPhotoCleaner.clean(any()) } just runs
            every { memberPhotoProcessor.process(any(), photoFile) } returns processed
            val capturedNewPhoto = slot<NewPhoto>()
            every { memberPhotoRepository.save(capture(capturedNewPhoto)) } returns 1L

            // When
            service.upload(email, photoFile)

            // Then
            verify { memberPhotoCleaner.clean(Email(email)) }
            capturedNewPhoto.captured.filePath shouldBe processed.filePath
            capturedNewPhoto.captured.thumbnailPath shouldBe processed.thumbnailPath
            capturedNewPhoto.captured.memberEmail shouldBe Email(email)
        }
    }

    context("delete") {

        test("cleaner로 위임한다") {
            // Given
            val email = "user@example.com"
            every { memberPhotoCleaner.clean(any()) } just runs

            // When
            service.delete(email)

            // Then
            verify { memberPhotoCleaner.clean(Email(email)) }
        }
    }
})
