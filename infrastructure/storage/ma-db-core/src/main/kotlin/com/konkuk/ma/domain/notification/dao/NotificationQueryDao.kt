package com.konkuk.ma.domain.notification.dao

import com.konkuk.ma.domain.common.domain.page.CursorIdCondition
import com.konkuk.ma.domain.common.domain.page.CursorResult
import com.konkuk.ma.domain.notification.domain.Notification
import com.konkuk.ma.domain.notification.entity.NotificationEntity
import com.konkuk.ma.domain.notification.entity.table.NotificationTable
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class NotificationQueryDao {
    fun findOne(id: Long): NotificationEntity? {
        return NotificationTable
            .activeRows { NotificationTable.id eq id }
            .limit(1)
            .firstOrNull()
            ?.let { NotificationEntity.from(it) }
    }

    fun findByRecipient(recipientId: Long, cursor: CursorIdCondition): CursorResult<List<Notification>> {
        val cursorId = cursor.cursorId
        val notifications = NotificationTable
            .selectAll()
            .where {
                var condition: Op<Boolean> = (NotificationTable.recipientId eq recipientId) and
                    (NotificationTable.deleted eq false)
                if (cursorId != null) {
                    condition = condition and (NotificationTable.id less cursorId)
                }
                condition
            }
            .orderBy(NotificationTable.id to SortOrder.DESC)
            .limit(cursor.size)
            .map { NotificationEntity.from(it).toDomain() }

        return CursorResult.of(notifications, cursor.size) { it.id }
    }

    fun countUnread(recipientId: Long): Long {
        return NotificationTable
            .selectAll()
            .where {
                (NotificationTable.recipientId eq recipientId) and
                    (NotificationTable.read eq false) and
                    (NotificationTable.deleted eq false)
            }
            .count()
    }
}
