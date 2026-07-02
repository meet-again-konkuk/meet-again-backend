package com.konkuk.ma.domain.xroom.domain.media

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class MemoryPhotoUrlsTest : FunSpec({

    context("photoUrlOf") {

        test("사진 URL이 존재하는 기억은 해당 URL을 반환한다") {
            val memoryPhotoUrls = MemoryPhotoUrls(
                mapOf(
                    1L to "/files/memory/memory-photo/1/photo.jpg",
                    2L to "/files/memory/memory-photo/2/photo.jpg",
                ),
            )

            memoryPhotoUrls.photoUrlOf(1L) shouldBe "/files/memory/memory-photo/1/photo.jpg"
            memoryPhotoUrls.photoUrlOf(2L) shouldBe "/files/memory/memory-photo/2/photo.jpg"
        }

        test("사진 URL이 없는 기억은 null을 반환한다") {
            val memoryPhotoUrls = MemoryPhotoUrls(mapOf(1L to "/files/memory/memory-photo/1/photo.jpg"))

            memoryPhotoUrls.photoUrlOf(999L).shouldBeNull()
        }

        test("빈 맵이면 모든 기억에 대해 null을 반환한다") {
            val memoryPhotoUrls = MemoryPhotoUrls(emptyMap())

            memoryPhotoUrls.photoUrlOf(1L).shouldBeNull()
        }
    }
})
