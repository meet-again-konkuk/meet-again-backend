package com.konkuk.ma.domain.notification.api.response

import com.konkuk.ma.domain.notification.domain.NotificationType
import com.konkuk.ma.domain.notification.domain.NotificationWithActor
import java.time.LocalDateTime

class NotificationResponse(
    val id: Long,
    val type: NotificationType,
    val actorId: Long,
    val actorNickname: String,
    val postId: Long,
    val commentId: Long,
    val read: Boolean,
    val createdDate: LocalDateTime,
) {
    companion object {
        fun from(notificationWithActor: NotificationWithActor): NotificationResponse {
            val notification = notificationWithActor.notification
            return NotificationResponse(
                id = notification.id,
                type = notification.type,
                actorId = notification.actorId,
                actorNickname = notificationWithActor.actorNickname,
                postId = notification.postId,
                commentId = notification.commentId,
                read = notification.read,
                createdDate = notification.createdDate,
            )
        }
    }
}
