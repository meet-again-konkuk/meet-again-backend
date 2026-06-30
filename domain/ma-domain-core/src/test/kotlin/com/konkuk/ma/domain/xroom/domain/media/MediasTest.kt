package com.konkuk.ma.domain.xroom.domain.media

import com.konkuk.ma.domain.xroom.fixture.MediaFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MediasTest : FunSpec({

    context("findByMemory") {

        test("해당 memoryId의 미디어를 반환한다") {
            // Given
            val media = MediaFixture.create(id = 1L, memoryId = 10L)
            val medias = Medias(listOf(media))

            // When
            val found = medias.findByMemory(media.memoryId)

            // Then
            found shouldBe media
        }

        test("여러 미디어 중 정확히 일치하는 memoryId의 미디어를 반환한다") {
            // Given
            val target = MediaFixture.create(id = 1L, memoryId = 10L)
            val other = MediaFixture.create(id = 2L, memoryId = 20L)
            val medias = Medias(listOf(other, target))

            // When
            val found = medias.findByMemory(target.memoryId)

            // Then
            found shouldBe target
        }

        test("해당 memoryId의 미디어가 없으면 null을 반환한다") {
            // Given
            val media = MediaFixture.create(memoryId = 10L)
            val medias = Medias(listOf(media))

            // When
            val found = medias.findByMemory(media.memoryId + 1)

            // Then
            found shouldBe null
        }

        test("빈 컬렉션에서 조회하면 null을 반환한다") {
            // Given
            val medias = Medias(emptyList())

            // When
            val found = medias.findByMemory(1L)

            // Then
            found shouldBe null
        }
    }
})
