package com.konkuk.ma.domain.notification.fixture

import com.konkuk.ma.domain.notification.domain.Notification
import com.konkuk.ma.domain.notification.domain.NotificationType
import java.time.LocalDateTime

object NotificationFixture {
    fun create(
        id: Long = 1L,
        recipientId: Long = 1L,
        type: NotificationType = NotificationType.COMMENT_ON_POST,
        actorId: Long = 2L,
        postId: Long = 10L,
        commentId: Long = 100L,
        read: Boolean = false,
        createdDate: LocalDateTime = LocalDateTime.now(),
    ): Notification {
        return Notification(
            id = id,
            recipientId = recipientId,
            type = type,
            actorId = actorId,
            postId = postId,
            commentId = commentId,
            read = read,
            createdDate = createdDate,
        )
    }
}
