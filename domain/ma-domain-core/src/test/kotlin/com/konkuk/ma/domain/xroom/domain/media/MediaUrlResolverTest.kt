package com.konkuk.ma.domain.xroom.domain.media

import com.konkuk.ma.domain.common.domain.file.port.FileUrlResolver
import com.konkuk.ma.domain.xroom.fixture.MediaFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk

class MediaUrlResolverTest : FunSpec({

    val fileUrlResolver = mockk<FileUrlResolver>()
    val resolver = MediaUrlResolver(fileUrlResolver)

    beforeEach {
        clearAllMocks()
        every { fileUrlResolver.resolve(any()) } answers { "/resolved/${firstArg<String>()}" }
    }

    context("resolve") {

        test("원본과 썸네일 키를 각각 URL로 변환한다") {
            // Given
            val media = MediaFixture.create(
                storageKey = "memory/memory-photo/1/photo.jpg",
                thumbnailKey = "memory/thumbnail/1/thumb_photo.jpg",
            )

            // When
            val urls = resolver.resolve(media)

            // Then
            urls.photoUrl shouldBe "/resolved/${media.storageKey}"
            urls.thumbnailUrl shouldBe "/resolved/${media.thumbnailKey}"
        }

        test("썸네일 키가 없으면 thumbnailUrl은 null이다") {
            // Given
            val media = MediaFixture.create(thumbnailKey = null)

            // When
            val urls = resolver.resolve(media)

            // Then
            urls.photoUrl shouldBe "/resolved/${media.storageKey}"
            urls.thumbnailUrl.shouldBeNull()
        }
    }

    context("resolveByMemory") {

        test("기억별로 원본 storageKey를 URL로 변환해 담는다") {
            // Given
            val first = MediaFixture.create(memoryId = 1L, storageKey = "memory/memory-photo/1/a.jpg")
            val second = MediaFixture.create(memoryId = 2L, storageKey = "memory/memory-photo/2/b.jpg")

            // When
            val photoUrls = resolver.resolveByMemory(listOf(first, second))

            // Then
            photoUrls.photoUrlOf(first.memoryId) shouldBe "/resolved/${first.storageKey}"
            photoUrls.photoUrlOf(second.memoryId) shouldBe "/resolved/${second.storageKey}"
        }

        test("빈 목록이면 어떤 기억의 URL도 없다") {
            // When
            val photoUrls = resolver.resolveByMemory(emptyList())

            // Then
            photoUrls.photoUrlOf(1L).shouldBeNull()
        }
    }
})
