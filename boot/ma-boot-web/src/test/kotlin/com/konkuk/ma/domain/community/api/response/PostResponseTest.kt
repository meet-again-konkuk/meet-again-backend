package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.community.domain.Post
import com.konkuk.ma.domain.community.domain.PostCategory
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class PostResponseTest : FunSpec({

    context("from") {

        test("Post 도메인 객체를 PostResponse로 변환한다") {
            val now = LocalDateTime.of(2026, 4, 6, 12, 0, 0)
            val post = Post(
                id = 1L,
                authorEmail = "author@example.com",
                authorNickname = "작성자",
                category = PostCategory.SUCCESS_STORY,
                title = "제목",
                content = "내용",
                likes = 5,
                comments = 3,
                createdDate = now.minusHours(2),
            )

            val response = PostResponse.from(post)

            response.id shouldBe post.id
            response.nickname shouldBe post.authorNickname
            response.category shouldBe post.category.name
            response.title shouldBe post.title
            response.content shouldBe post.content
            response.likes shouldBe post.likes
            response.comments shouldBe post.comments
        }

        test("timeAgo는 도메인 객체의 calculateTimeAgo 결과를 사용한다") {
            val now = LocalDateTime.now()
            val post = Post(
                id = 1L,
                authorEmail = "author@example.com",
                authorNickname = "작성자",
                category = PostCategory.CHEER,
                title = "제목",
                content = "내용",
                createdDate = now.minusMinutes(30),
            )

            val response = PostResponse.from(post)

            response.timeAgo shouldBe post.calculateTimeAgo()
        }
    }
})
