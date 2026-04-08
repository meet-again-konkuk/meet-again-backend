package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.common.domain.TimeAgoCalculator
import com.konkuk.ma.domain.community.domain.Post
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.PostWithAuthor
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class PostResponseTest : FunSpec({

    context("from") {

        test("PostWithAuthor 도메인 객체를 PostResponse로 변환한다") {
            val now = LocalDateTime.of(2026, 4, 6, 12, 0, 0)
            val postWithAuthor = PostWithAuthor(
                post = Post(
                    id = 1L,
                    authorEmail = "author@example.com",
                    category = PostCategory.SUCCESS_STORY,
                    title = "제목",
                    content = "내용",
                    likes = 5,
                    comments = 3,
                    createdDate = now.minusHours(2),
                ),
                nickname = "작성자",
            )

            val response = PostResponse.from(postWithAuthor)

            response.id shouldBe postWithAuthor.post.id
            response.nickname shouldBe postWithAuthor.nickname
            response.category shouldBe postWithAuthor.post.category.name
            response.title shouldBe postWithAuthor.post.title
            response.content shouldBe postWithAuthor.post.content
            response.likes shouldBe postWithAuthor.post.likes
            response.comments shouldBe postWithAuthor.post.comments
        }

        test("timeAgo는 TimeAgoCalculator 결과를 사용한다") {
            val now = LocalDateTime.now()
            val postWithAuthor = PostWithAuthor(
                post = Post(
                    id = 1L,
                    authorEmail = "author@example.com",
                    category = PostCategory.CHEER,
                    title = "제목",
                    content = "내용",
                    likes = 0,
                    comments = 0,
                    createdDate = now.minusMinutes(30),
                ),
                nickname = "작성자",
            )

            val response = PostResponse.from(postWithAuthor)

            response.timeAgo shouldBe TimeAgoCalculator.calculate(postWithAuthor.post.createdDate)
        }
    }
})
