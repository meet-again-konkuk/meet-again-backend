package com.konkuk.ma.domain.notification.api

import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.notification.application.NotificationCommandService
import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.patchJson
import com.konkuk.ma.extension.pathVariables
import com.konkuk.ma.vocabulary.notificationIdPath
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc

@WebMvcTest(NotificationCommandApi::class)
@BaseApiTest
class NotificationCommandApiTest(
    private val mockMvc: MockMvc,
    @MockkBean private val notificationCommandService: NotificationCommandService,
) : FunSpec({

    test("단건 알림 읽음 처리 API 문서화") {
        // Given
        every { notificationCommandService.markAsRead(any(), any()) } just runs

        // When & Then
        mockMvc.patchJson("/api/notifications/{notificationId}/read", 1L)
            .andExpect { status { isOk() } }
            .andDocument(
                "notification/read-notification",
                pathVariables(
                    notificationIdPath(),
                ),
            )
    }

    test("본인의 알림이 아니면 403을 반환한다") {
        // Given
        every { notificationCommandService.markAsRead(any(), any()) } throws
            AccessDeniedException(EntityType.NOTIFICATION, "2", "1")

        // When & Then
        mockMvc.patchJson("/api/notifications/{notificationId}/read", 1L)
            .andExpect { status { isForbidden() } }
    }

    test("존재하지 않는 알림이면 404를 반환한다") {
        // Given
        every { notificationCommandService.markAsRead(any(), any()) } throws
            EntityNotFoundException(EntityType.NOTIFICATION, "999")

        // When & Then
        mockMvc.patchJson("/api/notifications/{notificationId}/read", 999L)
            .andExpect { status { isNotFound() } }
    }

    test("전체 알림 읽음 처리 API 문서화") {
        // Given
        every { notificationCommandService.markAllAsRead(any()) } just runs

        // When & Then
        mockMvc.patchJson("/api/notifications/read-all")
            .andExpect { status { isOk() } }
            .andDocument(
                "notification/read-all-notifications",
            )
    }
})
