package com.konkuk.ma.domain.community.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.community.application.CommentCommandService
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.pathVariables
import com.konkuk.ma.extension.requestBody
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.commentContent
import com.konkuk.ma.vocabulary.commentId
import com.konkuk.ma.vocabulary.parentCommentId
import com.konkuk.ma.vocabulary.postIdPath
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(CommunityCommentCommandApi::class)
@BaseApiTest
class CommunityCommentCommandApiTest(
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
})
