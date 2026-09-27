package com.konkuk.ma.domain.notification.repository

import com.konkuk.ma.domain.common.domain.page.CursorIdCondition
import com.konkuk.ma.domain.common.domain.page.CursorResult
import com.konkuk.ma.domain.notification.dao.NotificationQueryDao
import com.konkuk.ma.domain.notification.domain.Notification
import com.konkuk.ma.domain.notification.domain.port.NotificationQueryRepository
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Repository

@Repository
class NotificationQueryCoreRepository(
    private val notificationQueryDao: NotificationQueryDao,
) : NotificationQueryRepository {
    override fun findOne(id: Long): Notification {
        return notificationQueryDao.findOne(id)?.toDomain()
            ?: throw EntityNotFoundException(EntityType.NOTIFICATION, id.toString())
    }

    override fun findByRecipient(recipientId: Long, cursor: CursorIdCondition): CursorResult<List<Notification>> {
        return notificationQueryDao.findByRecipient(recipientId, cursor)
    }

    override fun countUnread(recipientId: Long): Long {
        return notificationQueryDao.countUnread(recipientId)
    }
}
