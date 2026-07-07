package com.konkuk.ma.domain.community.api

import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.common.domain.page.CursorIdCondition
import com.konkuk.ma.domain.common.domain.page.CursorResult
import com.konkuk.ma.domain.community.application.PostQueryService
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.PostWithAuthor
import com.konkuk.ma.domain.community.fixture.PostFixture
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.getJson
import com.konkuk.ma.extension.requestParam
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.categoryParam
import com.konkuk.ma.vocabulary.cursorIdParam
import com.konkuk.ma.vocabulary.postCategory
import com.konkuk.ma.vocabulary.postCommentCount
import com.konkuk.ma.vocabulary.postContent
import com.konkuk.ma.vocabulary.postId
import com.konkuk.ma.vocabulary.postImageUrl
import com.konkuk.ma.vocabulary.postIsMine
import com.konkuk.ma.vocabulary.postLikedByMe
import com.konkuk.ma.vocabulary.postLikes
import com.konkuk.ma.vocabulary.postNickname
import com.konkuk.ma.vocabulary.postThumbnailUrl
import com.konkuk.ma.vocabulary.postTimeAgo
import com.konkuk.ma.vocabulary.postTitle
import com.konkuk.ma.vocabulary.postsHasNext
import com.konkuk.ma.vocabulary.postsNextCursorId
import com.konkuk.ma.vocabulary.sizeParam
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import java.time.LocalDateTime

@WebMvcTest(PostQueryApi::class)
@BaseApiTest
class PostQueryApiTest(
    private val mockMvc: MockMvc,
    @MockkBean private val postQueryService: PostQueryService,
) : FunSpec({

    test("커뮤니티 게시글 목록 조회 API 문서화") {
        // Given
        val postWithAuthor = PostWithAuthor(
            post = PostFixture.create(category = PostCategory.CHEER),
            nickname = "테스트닉네임",
            likeCount = 5,
            likedByMe = true,
            isMine = false,
            commentCount = 3,
            imageUrl = "/files/community/post/1/image.jpg",
            thumbnailUrl = "/files/community/post/1/thumb.jpg",
        )
        val cursorResult = CursorResult(
            data = listOf(postWithAuthor),
            hasNext = true,
            nextCursorId = 1L,
        )

        every { postQueryService.find(PostCategory.CHEER, CursorIdCondition(null, 20), any<Long>()) } returns cursorResult

        // When & Then
        mockMvc.getJson("/api/community/posts") {
            param("category", "CHEER")
            param("size", "20")
        }
            .andExpect { status { isOk() } }
            .andDocument(
                "community/find-posts",
                requestParam(
                    categoryParam(),
                    cursorIdParam(),
                    sizeParam(),
                ),
                responseBody(
                    postId(),
                    postNickname(),
                    postCategory(),
                    postTitle(),
                    postContent(),
                    postLikes(),
                    postTimeAgo(),
                    postLikedByMe(),
                    postIsMine(),
                    postCommentCount(),
                    postImageUrl("data[].imageUrl") isOptional true,
                    postThumbnailUrl("data[].thumbnailUrl") isOptional true,
                    postsHasNext(),
                    postsNextCursorId(),
                ),
            )
    }

    test("게시글이 없는 경우 빈 목록 반환") {
        // Given
        val cursorResult: CursorResult<List<PostWithAuthor>> = CursorResult(
            data = emptyList(),
            hasNext = false,
            nextCursorId = null,
        )

        every { postQueryService.find(PostCategory.SUCCESS_STORY, CursorIdCondition(null, 20), any<Long>()) } returns cursorResult

        // When & Then
        mockMvc.getJson("/api/community/posts") {
            param("category", "SUCCESS_STORY")
        }
            .andExpect { status { isOk() } }
            .andDocument(
                "community/find-posts-empty",
            )
    }
})
