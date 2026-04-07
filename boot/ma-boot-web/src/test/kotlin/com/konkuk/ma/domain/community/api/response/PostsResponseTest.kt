package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.common.domain.page.PageResult
import com.konkuk.ma.domain.community.domain.Post
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.Posts
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class PostsResponseTest : FunSpec({

    context("from") {

        test("PageResult를 PostsResponse로 변환한다") {
            val post = Post(
                id = 1L,
                authorEmail = "author@example.com",
                authorNickname = "작성자",
                category = PostCategory.SUCCESS_STORY,
                title = "제목",
                content = "내용",
                likes = 5,
                comments = 3,
                createdDate = LocalDateTime.now().minusHours(1),
            )
            val pageResult = PageResult(
                data = Posts(listOf(post)),
                totalCount = 1L,
                currentPage = 0,
                pageSize = 20,
            )

            val response = PostsResponse.from(pageResult)

            response.posts shouldHaveSize 1
            response.hasNext shouldBe false
        }

        test("빈 게시글 목록을 변환하면 빈 응답을 반환한다") {
            val pageResult = PageResult(
                data = Posts(emptyList()),
                totalCount = 0L,
                currentPage = 0,
                pageSize = 20,
            )

            val response = PostsResponse.from(pageResult)

            response.posts shouldHaveSize 0
            response.hasNext shouldBe false
        }

        test("hasNext가 true인 경우 응답에 반영된다") {
            val postList = List(20) { index ->
                Post(
                    id = index.toLong(),
                    authorEmail = "author@example.com",
                    authorNickname = "작성자",
                    category = PostCategory.COUNSELING,
                    title = "제목 $index",
                    content = "내용 $index",
                    createdDate = LocalDateTime.now(),
                )
            }
            val pageResult = PageResult(
                data = Posts(postList),
                totalCount = 20.toLong() + 1,
                currentPage = 0,
                pageSize = 20,
            )

            val response = PostsResponse.from(pageResult)

            response.hasNext shouldBe true
        }
    }
})
