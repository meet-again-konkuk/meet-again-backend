package com.konkuk.ma.domain.xroom.domain.media

import com.konkuk.ma.domain.xroom.fixture.MediaFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class MediaTest : FunSpec({

    context("belongsTo") {

        test("같은 memoryId면 true를 반환한다") {
            // Given
            val media = MediaFixture.create(memoryId = 10L)

            // When & Then
            media.belongsTo(media.memoryId).shouldBeTrue()
        }

        test("다른 memoryId면 false를 반환한다") {
            // Given
            val media = MediaFixture.create(memoryId = 10L)

            // When & Then
            media.belongsTo(media.memoryId + 1).shouldBeFalse()
        }
    }

    context("toPhotoUrl") {

        test("baseUrl과 storageKey를 결합해 사진 URL을 만든다") {
            // Given
            val media = MediaFixture.create(storageKey = "memory/memory-photo/1/photo.jpg")

            // When & Then
            media.toPhotoUrl("/files") shouldBe "/files/memory/memory-photo/1/photo.jpg"
        }
    }

    context("toThumbnailUrl") {

        test("thumbnailKey가 있으면 baseUrl과 결합해 썸네일 URL을 만든다") {
            // Given
            val media = MediaFixture.create(thumbnailKey = "memory/thumbnail/1/thumb_photo.jpg")

            // When & Then
            media.toThumbnailUrl("/files") shouldBe "/files/memory/thumbnail/1/thumb_photo.jpg"
        }

        test("thumbnailKey가 없으면 null을 반환한다") {
            // Given
            val media = MediaFixture.create(thumbnailKey = null)

            // When & Then
            media.toThumbnailUrl("/files").shouldBeNull()
        }
    }
})
