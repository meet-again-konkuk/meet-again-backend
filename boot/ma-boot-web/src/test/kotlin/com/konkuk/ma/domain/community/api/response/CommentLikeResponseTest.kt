package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.community.domain.CommentLikeResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CommentLikeResponseTest : FunSpec({

    context("from") {

        test("CommentLikeResult를 CommentLikeResponse로 변환한다 - 좋아요 추가") {
            // Given
            val result = CommentLikeResult(liked = true, likeCount = 5)

            // When
            val response = CommentLikeResponse.from(result)

            // Then
            response.liked shouldBe result.liked
            response.likeCount shouldBe result.likeCount
        }

        test("CommentLikeResult를 CommentLikeResponse로 변환한다 - 좋아요 취소") {
            // Given
            val result = CommentLikeResult(liked = false, likeCount = 0)

            // When
            val response = CommentLikeResponse.from(result)

            // Then
            response.liked shouldBe result.liked
            response.likeCount shouldBe result.likeCount
        }
    }
})
