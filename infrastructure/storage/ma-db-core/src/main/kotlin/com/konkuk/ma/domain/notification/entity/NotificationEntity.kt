package com.konkuk.ma.domain.notification.entity

import com.konkuk.ma.domain.notification.domain.Notification
import com.konkuk.ma.domain.notification.domain.NotificationType
import com.konkuk.ma.domain.notification.entity.table.NotificationTable
import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDateTime

class NotificationEntity(
    val id: Long,
    val recipientId: Long,
    val type: NotificationType,
    val actorId: Long,
    val postId: Long,
    val commentId: Long,
    val read: Boolean,
    val createdDate: LocalDateTime,
) {
    fun toDomain(): Notification {
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

    companion object {
        fun from(row: ResultRow): NotificationEntity {
            return NotificationEntity(
                id = row[NotificationTable.id].value,
                recipientId = row[NotificationTable.recipientId],
                type = NotificationType.valueOf(row[NotificationTable.type]),
                actorId = row[NotificationTable.actorId],
                postId = row[NotificationTable.postId],
                commentId = row[NotificationTable.commentId],
                read = row[NotificationTable.read],
                createdDate = row[NotificationTable.createdDate],
            )
        }
    }
}
