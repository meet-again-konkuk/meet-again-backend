package com.konkuk.ma.domain.common.domain.file

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class FileUrlsTest : FunSpec({

    context("urlOf") {

        test("담겨 있는 소유자의 URL 을 반환한다") {
            // Given
            val ownerId = 10L
            val url = "/files/member/thumbnail/10/thumb_photo.jpg"
            val fileUrls = FileUrls(mapOf(ownerId to url))

            // When & Then
            fileUrls.urlOf(ownerId) shouldBe url
        }

        test("담겨 있지 않은 소유자면 null 을 반환한다") {
            // Given
            val fileUrls = FileUrls(mapOf(10L to "/files/member/profile/10/photo.jpg"))

            // When & Then
            fileUrls.urlOf(99L).shouldBeNull()
        }

        test("빈 컬렉션이면 어떤 소유자도 null 이다") {
            // Given
            val fileUrls = FileUrls(emptyMap())

            // When & Then
            fileUrls.urlOf(1L).shouldBeNull()
        }

        test("여러 소유자가 담겨 있어도 각자의 URL 을 반환한다") {
            // Given
            val fileUrls = FileUrls(
                mapOf(
                    1L to "/files/memory/memory-photo/1/photo.jpg",
                    2L to "/files/memory/memory-photo/2/photo.jpg",
                ),
            )

            // When & Then
            fileUrls.urlOf(1L) shouldBe "/files/memory/memory-photo/1/photo.jpg"
            fileUrls.urlOf(2L) shouldBe "/files/memory/memory-photo/2/photo.jpg"
        }
    }
})
