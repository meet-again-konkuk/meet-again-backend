package com.konkuk.ma.domain.community.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.community.application.CommentCommandService
import com.konkuk.ma.domain.community.domain.NewComment
import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.EntityType
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.pathVariables
import com.konkuk.ma.extension.requestBody
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.commentContent
import com.konkuk.ma.vocabulary.commentId
import com.konkuk.ma.vocabulary.commentIdPath
import com.konkuk.ma.vocabulary.parentCommentId
import com.konkuk.ma.vocabulary.postIdPath
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(CommentCommandApi::class)
@BaseApiTest
class CommentCommandApiTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    @MockkBean private val commentCommandService: CommentCommandService,
) : FunSpec({

    test("일반 댓글 작성 API 문서화") {
        // Given
        val request = mapOf(
            "content" to "좋은 글이네요!",
        )

        every { commentCommandService.create(any()) } returns 1L

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/community/posts/{postId}/comments", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andDocument(
                "community/create-comment",
                pathVariables(
                    postIdPath(),
                ),
                requestBody(
                    commentContent(),
                    parentCommentId(),
                ),
                responseBody(
                    commentId(),
                ),
            )
    }

    test("대댓글 작성 API 문서화") {
        // Given
        val request = mapOf(
            "content" to "저도 동감합니다!",
            "parentCommentId" to 1L,
        )

        every { commentCommandService.create(any()) } returns 2L

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/community/posts/{postId}/comments", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andDocument(
                "community/create-reply-comment",
                pathVariables(
                    postIdPath(),
                ),
                requestBody(
                    commentContent(),
                    parentCommentId(),
                ),
                responseBody(
                    commentId(),
                ),
            )
    }

    test("댓글 내용이 최대 글자수를 초과하면 400을 반환한다") {
        // Given
        val overLengthContent = "a".repeat(NewComment.MAX_CONTENT_LENGTH + 1)
        val request = mapOf("content" to overLengthContent)

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/community/posts/{postId}/comments", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    test("댓글 내용이 비어있으면 400을 반환한다") {
        // Given
        val request = mapOf("content" to " ")

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.post("/api/community/posts/{postId}/comments", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    test("댓글 삭제 API 문서화") {
        // Given
        every { commentCommandService.delete(any(), any()) } just runs

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.delete(
                "/api/community/posts/{postId}/comments/{commentId}", 1L, 1L
            )
        )
            .andExpect(status().isOk)
            .andDocument(
                "community/delete-comment",
                pathVariables(
                    postIdPath(),
                    commentIdPath(),
                ),
            )
    }

    test("소유권이 없는 댓글 삭제 시 403을 반환한다") {
        // Given
        every { commentCommandService.delete(any(), any()) } throws
            AccessDeniedException(EntityType.COMMUNITY_COMMENT, "owner@example.com", "holeman@naver.com")

        // When & Then
        mockMvc.perform(
            RestDocumentationRequestBuilders.delete(
                "/api/community/posts/{postId}/comments/{commentId}", 1L, 1L
            )
        )
            .andExpect(status().isForbidden)
    }
})
