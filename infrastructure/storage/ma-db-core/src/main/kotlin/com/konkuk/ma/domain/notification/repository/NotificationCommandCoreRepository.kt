package com.konkuk.ma.domain.notification.repository

import com.konkuk.ma.domain.notification.dao.NotificationCommandDao
import com.konkuk.ma.domain.notification.domain.NewNotification
import com.konkuk.ma.domain.notification.domain.port.NotificationCommandRepository
import org.springframework.stereotype.Repository

@Repository
class NotificationCommandCoreRepository(
    private val notificationCommandDao: NotificationCommandDao,
) : NotificationCommandRepository {
    override fun save(newNotification: NewNotification): Long {
        return notificationCommandDao.save(newNotification)
    }

    override fun markAsRead(notificationId: Long) {
        notificationCommandDao.markAsRead(notificationId)
    }

    override fun markAllAsRead(recipientId: Long) {
        notificationCommandDao.markAllAsRead(recipientId)
    }

    override fun deleteByMember(recipientId: Long) {
        notificationCommandDao.deleteByMember(recipientId)
    }
}
