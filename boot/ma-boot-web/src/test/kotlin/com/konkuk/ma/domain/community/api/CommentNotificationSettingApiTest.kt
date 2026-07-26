package com.konkuk.ma.domain.community.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.community.application.CommentNotificationSettingCommandService
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.patchJson
import com.konkuk.ma.extension.pathVariables
import com.konkuk.ma.extension.requestBody
import com.konkuk.ma.vocabulary.commentNotificationEnabled
import com.konkuk.ma.vocabulary.postIdPath
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc

@WebMvcTest(CommentNotificationSettingApi::class)
@BaseApiTest
class CommentNotificationSettingApiTest(
    private val mockMvc: MockMvc,
    private val mapper: ObjectMapper,
    @MockkBean private val commentNotificationSettingCommandService: CommentNotificationSettingCommandService,
) : FunSpec({

    test("게시글 댓글 알림 설정 API 문서화") {
        // Given
        val request = mapOf("enabled" to true)

        every { commentNotificationSettingCommandService.set(any(), any(), any()) } just runs

        // When & Then
        mockMvc.patchJson("/api/community/posts/{postId}/comment-notification", 1L) {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isOk() } }
            .andDocument(
                "community/set-comment-notification",
                pathVariables(
                    postIdPath(),
                ),
                requestBody(
                    commentNotificationEnabled(),
                ),
            )
    }

    test("존재하지 않는 게시글이면 404를 반환한다") {
        // Given
        val request = mapOf("enabled" to true)

        every { commentNotificationSettingCommandService.set(any(), any(), any()) } throws
            EntityNotFoundException(EntityType.COMMUNITY_POST, "999")

        // When & Then
        mockMvc.patchJson("/api/community/posts/{postId}/comment-notification", 999L) {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isNotFound() } }
    }

    test("enabled 값이 없으면 400을 반환한다") {
        // Given
        val request = mapOf("enabled" to null)

        // When & Then
        mockMvc.patchJson("/api/community/posts/{postId}/comment-notification", 1L) {
            content = mapper.writeValueAsString(request)
        }
            .andExpect { status { isBadRequest() } }
    }
})
