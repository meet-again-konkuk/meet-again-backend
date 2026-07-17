package com.konkuk.ma.domain.notification.domain

class NewNotification(
    val recipientId: Long,
    val type: NotificationType,
    val actorId: Long,
    val postId: Long,
    val commentId: Long,
)
