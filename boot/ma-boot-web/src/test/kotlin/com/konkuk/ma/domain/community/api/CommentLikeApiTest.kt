package com.konkuk.ma.domain.community.api

import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.community.application.CommentLikeService
import com.konkuk.ma.domain.community.domain.CommentLikeResult
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.pathVariables
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.support.security.WithAuthMember
import com.konkuk.ma.vocabulary.commentIdPath
import com.konkuk.ma.vocabulary.commentLikeCount
import com.konkuk.ma.vocabulary.commentLiked
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(CommentLikeApi::class)
@BaseApiTest
@WithAuthMember(email = "test@example.com")
class CommentLikeApiTest(
    private val mockMvc: MockMvc,
    @MockkBean private val commentLikeService: CommentLikeService,
) : FunSpec({

    test("댓글 좋아요 추가 API 문서화") {
        // Given
        every { commentLikeService.like(1L, "test@example.com") } returns
            CommentLikeResult(liked = true, likeCount = 3)

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/community/comments/{commentId}/likes", 1L)
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isCreated)
            .andDocument(
                "community/like-comment",
                pathVariables(
                    commentIdPath(),
                ),
                responseBody(
                    commentLiked(),
                    commentLikeCount(),
                ),
            )
    }

    test("댓글 좋아요 취소 API 문서화") {
        // Given
        every { commentLikeService.unlike(1L, "test@example.com") } returns
            CommentLikeResult(liked = false, likeCount = 2)

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.delete("/api/community/comments/{commentId}/likes", 1L)
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andDocument(
                "community/unlike-comment",
                pathVariables(
                    commentIdPath(),
                ),
                responseBody(
                    commentLiked(),
                    commentLikeCount(),
                ),
            )
    }
})
