package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.common.domain.page.CursorResult
import com.konkuk.ma.domain.community.domain.Post
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.Posts
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class PostsResponseTest : FunSpec({

    context("from") {

        test("CursorResult를 PostsResponse로 변환한다") {
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
            val cursorResult = CursorResult(
                data = Posts(listOf(post)),
                hasNext = false,
                nextCursorId = null,
            )

            val response = PostsResponse.from(cursorResult)

            response.posts shouldHaveSize 1
            response.hasNext shouldBe false
            response.nextCursorId.shouldBeNull()
        }

        test("빈 게시글 목록을 변환하면 빈 응답을 반환한다") {
            val cursorResult = CursorResult(
                data = Posts(emptyList()),
                hasNext = false,
                nextCursorId = null,
            )

            val response = PostsResponse.from(cursorResult)

            response.posts shouldHaveSize 0
            response.hasNext shouldBe false
            response.nextCursorId.shouldBeNull()
        }

        test("다음 페이지가 있으면 hasNext와 nextCursorId가 응답에 반영된다") {
            val postList = List(20) { index ->
                Post(
                    id = (20 - index).toLong(),
                    authorEmail = "author@example.com",
                    authorNickname = "작성자",
                    category = PostCategory.COUNSELING,
                    title = "제목 $index",
                    content = "내용 $index",
                    createdDate = LocalDateTime.now(),
                )
            }
            val cursorResult = CursorResult(
                data = Posts(postList),
                hasNext = true,
                nextCursorId = 1L,
            )

            val response = PostsResponse.from(cursorResult)

            response.hasNext shouldBe true
            response.nextCursorId shouldBe 1L
        }
    }
})
