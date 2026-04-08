package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.common.domain.TimeAgoCalculator
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.PostWithAuthor
import com.konkuk.ma.domain.community.fixture.PostFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class PostResponseTest : FunSpec({

    context("from") {

        test("PostWithAuthor 도메인 객체를 PostResponse로 변환한다") {
            val postWithAuthor = PostWithAuthor(
                post = PostFixture.create(category = PostCategory.SUCCESS_STORY, likes = 5),
                nickname = "작성자",
            )

            val response = PostResponse.from(postWithAuthor)

            response.id shouldBe postWithAuthor.post.id
            response.nickname shouldBe postWithAuthor.nickname
            response.category shouldBe postWithAuthor.post.category.name
            response.title shouldBe postWithAuthor.post.title
            response.content shouldBe postWithAuthor.post.content
            response.likes shouldBe postWithAuthor.post.likes
        }

        test("timeAgo는 TimeAgoCalculator 결과를 사용한다") {
            val postWithAuthor = PostWithAuthor(
                post = PostFixture.create(createdDate = LocalDateTime.now().minusMinutes(30)),
                nickname = "작성자",
            )

            val response = PostResponse.from(postWithAuthor)

            response.timeAgo shouldBe TimeAgoCalculator.calculate(postWithAuthor.post.createdDate)
        }
    }
})
