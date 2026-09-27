package com.konkuk.ma.domain.notification.domain.port

import com.konkuk.ma.domain.common.domain.page.CursorIdCondition
import com.konkuk.ma.domain.common.domain.page.CursorResult
import com.konkuk.ma.domain.notification.domain.Notification

interface NotificationQueryRepository {
    fun findOne(id: Long): Notification
    fun findByRecipient(recipientId: Long, cursor: CursorIdCondition): CursorResult<List<Notification>>
    fun countUnread(recipientId: Long): Long
}
