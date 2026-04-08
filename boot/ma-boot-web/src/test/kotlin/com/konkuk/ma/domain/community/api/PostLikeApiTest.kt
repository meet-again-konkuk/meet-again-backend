package com.konkuk.ma.domain.community.api

import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.community.application.PostLikeService
import com.konkuk.ma.domain.community.domain.PostLikeResult
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.pathVariables
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.support.security.WithAuthMember
import com.konkuk.ma.vocabulary.postIdPath
import com.konkuk.ma.vocabulary.postLikeCount
import com.konkuk.ma.vocabulary.postLiked
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(PostLikeApi::class)
@BaseApiTest
@WithAuthMember(email = "test@example.com")
class PostLikeApiTest(
    private val mockMvc: MockMvc,
    @MockkBean private val postLikeService: PostLikeService,
) : FunSpec({

    test("게시글 좋아요 추가 API 문서화") {
        // Given
        every { postLikeService.like(1L, "test@example.com") } returns
            PostLikeResult(liked = true, likeCount = 3)

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/community/posts/{postId}/likes", 1L)
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isCreated)
            .andDocument(
                "community/like-post",
                pathVariables(
                    postIdPath(),
                ),
                responseBody(
                    postLiked(),
                    postLikeCount(),
                ),
            )
    }

    test("게시글 좋아요 취소 API 문서화") {
        // Given
        every { postLikeService.unlike(1L, "test@example.com") } returns
            PostLikeResult(liked = false, likeCount = 2)

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.delete("/api/community/posts/{postId}/likes", 1L)
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andDocument(
                "community/unlike-post",
                pathVariables(
                    postIdPath(),
                ),
                responseBody(
                    postLiked(),
                    postLikeCount(),
                ),
            )
    }
})
