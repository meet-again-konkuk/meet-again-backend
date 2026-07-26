package com.konkuk.ma.domain.notification.domain.port

import com.konkuk.ma.domain.notification.domain.NewNotification

interface NotificationCommandRepository {
    fun save(newNotification: NewNotification): Long
    fun markAsRead(notificationId: Long)
    fun markAllAsRead(recipientId: Long)
    fun deleteByMember(recipientId: Long)
}
