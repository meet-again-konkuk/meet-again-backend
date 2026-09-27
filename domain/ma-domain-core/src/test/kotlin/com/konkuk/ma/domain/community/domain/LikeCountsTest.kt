package com.konkuk.ma.domain.community.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LikeCountsTest : FunSpec({

    context("countOf") {

        test("맵에 id가 존재하면 해당 좋아요 수를 반환한다") {
            // Given
            val likeCounts = LikeCounts.from(mapOf(1L to 3, 2L to 5))

            // When & Then
            likeCounts.countOf(1L) shouldBe 3
            likeCounts.countOf(2L) shouldBe 5
        }

        test("맵에 id가 없으면 0을 반환한다") {
            // Given
            val likeCounts = LikeCounts.from(mapOf(1L to 3))

            // When & Then
            likeCounts.countOf(999L) shouldBe 0
        }

        test("빈 맵에서 임의의 id를 조회하면 0을 반환한다") {
            // Given
            val likeCounts = LikeCounts.from(emptyMap())

            // When & Then
            likeCounts.countOf(1L) shouldBe 0
        }

        test("좋아요 수가 0으로 명시된 id는 0을 반환한다") {
            // Given - 집계 결과에 0이 들어있는 경우도 그대로 반환한다
            val likeCounts = LikeCounts.from(mapOf(1L to 0))

            // When & Then
            likeCounts.countOf(1L) shouldBe 0
        }
    }
})
