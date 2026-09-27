package com.konkuk.ma.domain.notification.application

import com.konkuk.ma.domain.notification.domain.port.NotificationCommandRepository
import com.konkuk.ma.domain.notification.domain.port.NotificationQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class NotificationCommandService(
    private val notificationQueryRepository: NotificationQueryRepository,
    private val notificationCommandRepository: NotificationCommandRepository,
) {
    fun markAsRead(notificationId: Long, memberId: Long) {
        val notification = notificationQueryRepository.findOne(notificationId)
        notification.validateRecipient(memberId)
        notificationCommandRepository.markAsRead(notificationId)
    }

    fun markAllAsRead(memberId: Long) {
        notificationCommandRepository.markAllAsRead(memberId)
    }
}
