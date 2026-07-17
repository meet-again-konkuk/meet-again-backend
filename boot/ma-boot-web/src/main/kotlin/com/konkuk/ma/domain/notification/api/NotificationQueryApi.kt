package com.konkuk.ma.domain.notification.api

import com.konkuk.ma.domain.common.domain.page.CursorIdCondition
import com.konkuk.ma.domain.notification.api.response.NotificationResponse
import com.konkuk.ma.domain.notification.api.response.UnreadCountResponse
import com.konkuk.ma.domain.notification.application.NotificationQueryService
import com.konkuk.ma.support.payload.response.CursorResponse
import com.konkuk.ma.support.security.LoginMember
import com.konkuk.ma.support.security.MemberInfo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/notifications")
class NotificationQueryApi(
    private val notificationQueryService: NotificationQueryService,
) {
    @GetMapping
    fun find(
        @LoginMember memberInfo: MemberInfo,
        @RequestParam(required = false) cursorId: Long?,
        @RequestParam(required = false) size: Int?,
    ): CursorResponse<List<NotificationResponse>> {
        val result = notificationQueryService.find(memberInfo.id, CursorIdCondition.of(cursorId, size))
        return CursorResponse(
            data = result.data.map { NotificationResponse.from(it) },
            hasNext = result.hasNext,
            nextCursorId = result.nextCursorId,
        )
    }

    @GetMapping("/unread-count")
    fun countUnread(@LoginMember memberInfo: MemberInfo): UnreadCountResponse {
        return UnreadCountResponse(notificationQueryService.countUnread(memberInfo.id))
    }
}
