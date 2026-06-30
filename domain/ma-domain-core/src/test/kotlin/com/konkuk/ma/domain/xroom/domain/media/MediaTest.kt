package com.konkuk.ma.domain.xroom.domain.media

import com.konkuk.ma.domain.xroom.fixture.MediaFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

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
})
