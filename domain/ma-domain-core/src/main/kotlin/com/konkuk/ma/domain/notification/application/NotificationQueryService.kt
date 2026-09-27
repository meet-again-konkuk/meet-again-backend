package com.konkuk.ma.domain.notification.application

import com.konkuk.ma.domain.common.domain.page.CursorIdCondition
import com.konkuk.ma.domain.common.domain.page.CursorResult
import com.konkuk.ma.domain.member.domain.Members
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.domain.notification.domain.NotificationWithActor
import com.konkuk.ma.domain.notification.domain.Notifications
import com.konkuk.ma.domain.notification.domain.port.NotificationQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class NotificationQueryService(
    private val notificationQueryRepository: NotificationQueryRepository,
    private val memberQueryRepository: MemberQueryRepository,
) {
    fun find(recipientId: Long, cursor: CursorIdCondition): CursorResult<List<NotificationWithActor>> {
        val result = notificationQueryRepository.findByRecipient(recipientId, cursor)
        val notifications = Notifications(result.data)
        val actors = Members(memberQueryRepository.findByIds(notifications.extractActorIds()))
        return CursorResult(notifications.combineWithActors(actors), result.hasNext, result.nextCursorId)
    }

    fun countUnread(recipientId: Long): Long {
        return notificationQueryRepository.countUnread(recipientId)
    }
}
