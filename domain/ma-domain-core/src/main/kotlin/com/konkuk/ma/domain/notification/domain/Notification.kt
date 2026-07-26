package com.konkuk.ma.domain.notification.domain

import com.konkuk.ma.domain.member.domain.Members
import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.EntityType
import java.time.LocalDateTime

class Notification(
    val id: Long = 0L,
    val recipientId: Long,
    val type: NotificationType,
    val actorId: Long,
    val postId: Long,
    val commentId: Long,
    val read: Boolean = false,
    val createdDate: LocalDateTime = LocalDateTime.now(),
) {
    fun validateRecipient(memberId: Long) {
        if (recipientId != memberId) {
            throw AccessDeniedException(EntityType.NOTIFICATION, recipientId.toString(), memberId.toString())
        }
    }

    fun withActor(actors: Members): NotificationWithActor {
        return NotificationWithActor(this, actors.findNickname(actorId))
    }
}
