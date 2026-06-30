package com.konkuk.ma.domain.xroom.domain.media

import com.konkuk.ma.domain.common.domain.file.port.FileStorage
import com.konkuk.ma.domain.common.domain.file.port.ThumbnailGenerator
import com.konkuk.ma.domain.common.fixture.PhotoFileFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk

class MediaProcessorTest : FunSpec({

    val fileStorage = mockk<FileStorage>()
    val thumbnailGenerator = mockk<ThumbnailGenerator>()
    val processor = MediaProcessor(fileStorage, thumbnailGenerator)

    beforeEach {
        clearAllMocks()
    }

    context("process") {

        test("store가 절대경로를 반환해도 leaf만 추출해 상대 storageKey로 반환한다") {
            // Given
            val memoryId = 1L
            val photoFile = PhotoFileFixture.create()
            val storedLeaf = "9f1c-uuid.jpg"
            // store는 디렉토리 prefix가 붙은 절대경로를 반환한다
            every {
                fileStorage.store(any(), photoFile)
            } returns "/var/data/uploads/memory/memory-photo/$memoryId/$storedLeaf"
            every { thumbnailGenerator.generate(photoFile.content, 400) } returns ByteArray(512)
            every { fileStorage.storeBytes(any(), any(), any()) } returns "/var/data/uploads/memory/thumbnail/$memoryId/thumb"

            // When
            val result = processor.process(memoryId, photoFile)

            // Then - 절대경로 prefix는 제거되고 directory + leaf 형태의 상대키가 된다
            result.storageKey shouldBe "memory/memory-photo/$memoryId/$storedLeaf"
        }

        test("썸네일 생성에 성공하면 thumbnailKey를 상대 경로로 반환한다") {
            // Given
            val memoryId = 1L
            val photoFile = PhotoFileFixture.create()
            every { fileStorage.store(any(), photoFile) } returns "/var/data/uploads/memory/memory-photo/$memoryId/uuid.jpg"
            every { thumbnailGenerator.generate(photoFile.content, 400) } returns ByteArray(512)
            every { fileStorage.storeBytes(any(), any(), any()) } returns "/var/data/uploads/memory/thumbnail/$memoryId/thumb"

            // When
            val result = processor.process(memoryId, photoFile)

            // Then
            result.thumbnailKey shouldBe "memory/thumbnail/$memoryId/thumb_${photoFile.originalFileName}"
        }

        test("썸네일 생성에 실패하면 thumbnailKey가 null인 ProcessedMedia를 반환한다") {
            // Given
            val memoryId = 1L
            val photoFile = PhotoFileFixture.create()
            every { fileStorage.store(any(), photoFile) } returns "/var/data/uploads/memory/memory-photo/$memoryId/uuid.jpg"
            every { thumbnailGenerator.generate(photoFile.content, 400) } throws RuntimeException("생성 실패")

            // When
            val result = processor.process(memoryId, photoFile)

            // Then - 원본은 저장되지만 썸네일은 best-effort로 null이 된다
            result.storageKey shouldNotBe null
            result.thumbnailKey shouldBe null
        }
    }
})
