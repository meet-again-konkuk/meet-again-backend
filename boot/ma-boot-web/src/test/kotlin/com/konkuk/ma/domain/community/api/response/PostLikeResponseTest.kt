package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.community.domain.PostLikeResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PostLikeResponseTest : FunSpec({

    context("from") {

        test("PostLikeResult를 PostLikeResponse로 변환한다 - 좋아요 추가") {
            // Given
            val result = PostLikeResult(liked = true, likeCount = 5)

            // When
            val response = PostLikeResponse.from(result)

            // Then
            response.liked shouldBe result.liked
            response.likeCount shouldBe result.likeCount
        }

        test("PostLikeResult를 PostLikeResponse로 변환한다 - 좋아요 취소") {
            // Given
            val result = PostLikeResult(liked = false, likeCount = 0)

            // When
            val response = PostLikeResponse.from(result)

            // Then
            response.liked shouldBe result.liked
            response.likeCount shouldBe result.likeCount
        }
    }
})
