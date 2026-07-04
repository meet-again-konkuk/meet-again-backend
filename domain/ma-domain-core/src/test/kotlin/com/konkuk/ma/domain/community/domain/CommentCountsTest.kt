package com.konkuk.ma.domain.community.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CommentCountsTest : FunSpec({

    context("countOf") {

        test("맵에 postId가 존재하면 해당 댓글 수를 반환한다") {
            // Given
            val commentCounts = CommentCounts.from(mapOf(1L to 3, 2L to 5))

            // When & Then
            commentCounts.countOf(1L) shouldBe 3
            commentCounts.countOf(2L) shouldBe 5
        }

        test("맵에 postId가 없으면 0을 반환한다") {
            // Given
            val commentCounts = CommentCounts.from(mapOf(1L to 3))

            // When & Then
            commentCounts.countOf(999L) shouldBe 0
        }

        test("빈 맵에서 임의의 postId를 조회하면 0을 반환한다") {
            // Given
            val commentCounts = CommentCounts.from(emptyMap())

            // When & Then
            commentCounts.countOf(1L) shouldBe 0
        }

        test("댓글 수가 0으로 명시된 postId는 0을 반환한다") {
            // Given - 집계 결과에 0이 들어있는 경우도 그대로 반환한다
            val commentCounts = CommentCounts.from(mapOf(1L to 0))

            // When & Then
            commentCounts.countOf(1L) shouldBe 0
        }
    }

    context("from") {

        test("countById 맵으로 생성하면 각 postId의 댓글 수를 조회할 수 있다") {
            // Given
            val countById = mapOf(1L to 2, 2L to 4)

            // When
            val commentCounts = CommentCounts.from(countById)

            // Then
            commentCounts.countOf(1L) shouldBe countById.getValue(1L)
            commentCounts.countOf(2L) shouldBe countById.getValue(2L)
        }

        test("빈 맵으로 생성해도 정상적으로 만들어지며 조회 시 0을 반환한다") {
            // Given & When
            val commentCounts = CommentCounts.from(emptyMap())

            // Then
            commentCounts.countOf(1L) shouldBe 0
        }
    }
})
