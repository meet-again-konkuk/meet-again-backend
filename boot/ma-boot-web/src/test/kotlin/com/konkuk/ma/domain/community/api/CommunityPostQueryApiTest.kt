package com.konkuk.ma.domain.community.api

import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.community.application.PostQueryService
import com.konkuk.ma.domain.community.domain.Post
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.Posts
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.getJson
import com.konkuk.ma.extension.requestParam
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.support.security.WithAuthMember
import com.konkuk.ma.vocabulary.categoryParam
import com.konkuk.ma.vocabulary.pageParam
import com.konkuk.ma.vocabulary.postCategory
import com.konkuk.ma.vocabulary.postComments
import com.konkuk.ma.vocabulary.postContent
import com.konkuk.ma.vocabulary.postId
import com.konkuk.ma.vocabulary.postLikes
import com.konkuk.ma.vocabulary.postNickname
import com.konkuk.ma.vocabulary.postTimeAgo
import com.konkuk.ma.vocabulary.postTitle
import com.konkuk.ma.vocabulary.postsCurrentPage
import com.konkuk.ma.vocabulary.postsHasNext
import com.konkuk.ma.vocabulary.postsTotalCount
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import java.time.LocalDateTime

@WebMvcTest(CommunityPostQueryApi::class)
@BaseApiTest
@WithAuthMember(email = "test@example.com")
class CommunityPostQueryApiTest(
    private val mockMvc: MockMvc,
    @MockkBean private val postQueryService: PostQueryService,
) : FunSpec({

    test("커뮤니티 게시글 목록 조회 API 문서화") {
        // Given
        val post = Post(
            id = 1L,
            authorEmail = "author@example.com",
            authorNickname = "테스트닉네임",
            category = PostCategory.CHEER,
            title = "안녕하세요",
            content = "반갑습니다",
            likes = 5,
            comments = 3,
            createdDate = LocalDateTime.now().minusMinutes(5),
        )
        val posts = Posts(
            data = listOf(post),
            totalCount = 100L,
            currentPage = 0,
        )

        every { postQueryService.find(PostCategory.CHEER, 0) } returns posts

        // When & Then
        mockMvc.getJson("/api/community/posts") {
            param("category", "CHEER")
            param("page", "0")
        }
            .andExpect { status { isOk() } }
            .andDocument(
                "community/find-posts",
                requestParam(
                    categoryParam(),
                    pageParam(),
                ),
                responseBody(
                    postId(),
                    postNickname(),
                    postCategory(),
                    postTitle(),
                    postContent(),
                    postLikes(),
                    postComments(),
                    postTimeAgo(),
                    postsTotalCount(),
                    postsCurrentPage(),
                    postsHasNext(),
                ),
            )
    }

    test("게시글이 없는 경우 빈 목록 반환") {
        // Given
        val emptyPosts = Posts(
            data = emptyList(),
            totalCount = 0L,
            currentPage = 0,
        )

        every { postQueryService.find(PostCategory.SUCCESS_STORY, 0) } returns emptyPosts

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
