package com.konkuk.ma.domain.notification.api

import com.konkuk.ma.config.BaseApiTest
import com.konkuk.ma.domain.common.domain.page.CursorResult
import com.konkuk.ma.domain.notification.application.NotificationQueryService
import com.konkuk.ma.domain.notification.domain.Notification
import com.konkuk.ma.domain.notification.domain.NotificationType
import com.konkuk.ma.domain.notification.domain.NotificationWithActor
import com.konkuk.ma.extension.andDocument
import com.konkuk.ma.extension.getJson
import com.konkuk.ma.extension.requestParam
import com.konkuk.ma.extension.responseBody
import com.konkuk.ma.vocabulary.notificationActorId
import com.konkuk.ma.vocabulary.notificationActorNickname
import com.konkuk.ma.vocabulary.notificationCommentId
import com.konkuk.ma.vocabulary.notificationCreatedDate
import com.konkuk.ma.vocabulary.notificationCursorIdParam
import com.konkuk.ma.vocabulary.notificationId
import com.konkuk.ma.vocabulary.notificationPostId
import com.konkuk.ma.vocabulary.notificationRead
import com.konkuk.ma.vocabulary.notificationType
import com.konkuk.ma.vocabulary.notificationsHasNext
import com.konkuk.ma.vocabulary.notificationsNextCursorId
import com.konkuk.ma.vocabulary.sizeParam
import com.konkuk.ma.vocabulary.unreadCount
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import java.time.LocalDateTime

@WebMvcTest(NotificationQueryApi::class)
@BaseApiTest
class NotificationQueryApiTest(
    private val mockMvc: MockMvc,
    @MockkBean private val notificationQueryService: NotificationQueryService,
) : FunSpec({

    test("알림 목록 조회 API 문서화") {
        // Given
        val notification = Notification(
            id = 12L,
            recipientId = 1L,
            type = NotificationType.COMMENT_ON_POST,
            actorId = 5L,
            postId = 30L,
            commentId = 77L,
            read = false,
            createdDate = LocalDateTime.of(2026, 7, 17, 10, 20, 30),
        )
        val cursorResult = CursorResult(
            data = listOf(NotificationWithActor(notification, "홍길동")),
            hasNext = true,
            nextCursorId = 12L,
        )

        every { notificationQueryService.find(any(), any()) } returns cursorResult

        // When & Then
        mockMvc.getJson("/api/notifications") {
            param("cursorId", "20")
            param("size", "20")
        }
            .andExpect { status { isOk() } }
            .andDocument(
                "notification/find-notifications",
                requestParam(
                    notificationCursorIdParam(),
                    sizeParam(),
                ),
                responseBody(
                    notificationId(),
                    notificationType(),
                    notificationActorId(),
                    notificationActorNickname(),
                    notificationPostId(),
                    notificationCommentId(),
                    notificationRead(),
                    notificationCreatedDate(),
                    notificationsHasNext(),
                    notificationsNextCursorId(),
                ),
            )
    }

    test("알림이 없는 경우 빈 목록을 반환한다") {
        // Given
        val cursorResult: CursorResult<List<NotificationWithActor>> = CursorResult(
            data = emptyList(),
            hasNext = false,
            nextCursorId = null,
        )

        every { notificationQueryService.find(any(), any()) } returns cursorResult

        // When & Then
        mockMvc.getJson("/api/notifications")
            .andExpect { status { isOk() } }
            .andDocument(
                "notification/find-notifications-empty",
            )
    }

    test("안읽음 알림 개수 조회 API 문서화") {
        // Given
        every { notificationQueryService.countUnread(any()) } returns 3L

        // When & Then
        mockMvc.getJson("/api/notifications/unread-count")
            .andExpect { status { isOk() } }
            .andDocument(
                "notification/unread-count",
                responseBody(
                    unreadCount(),
                ),
            )
    }
})
