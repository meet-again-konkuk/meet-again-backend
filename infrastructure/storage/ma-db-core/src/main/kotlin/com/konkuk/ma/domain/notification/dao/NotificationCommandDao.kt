package com.konkuk.ma.domain.notification.dao

import com.konkuk.ma.domain.notification.domain.NewNotification
import com.konkuk.ma.domain.notification.entity.table.NotificationTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Component

@Component
class NotificationCommandDao {
    fun save(newNotification: NewNotification): Long {
        return NotificationTable.insertAndGetId {
            it[recipientId] = newNotification.recipientId
            it[type] = newNotification.type.name
            it[actorId] = newNotification.actorId
            it[postId] = newNotification.postId
            it[commentId] = newNotification.commentId
        }.value
    }

    fun markAsRead(notificationId: Long) {
        NotificationTable.update({ NotificationTable.id eq notificationId }) {
            it[read] = true
        }
    }

    fun markAllAsRead(recipientId: Long) {
        NotificationTable.update({
            (NotificationTable.recipientId eq recipientId) and (NotificationTable.read eq false)
        }) {
            it[read] = true
        }
    }

    fun deleteByMember(recipientId: Long) {
        NotificationTable.deleteWhere { NotificationTable.recipientId eq recipientId }
    }
}
