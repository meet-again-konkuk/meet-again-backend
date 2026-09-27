package com.konkuk.ma.domain.notification.api

import com.konkuk.ma.domain.notification.application.NotificationCommandService
import com.konkuk.ma.support.security.LoginMember
import com.konkuk.ma.support.security.MemberInfo
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/notifications")
class NotificationCommandApi(
    private val notificationCommandService: NotificationCommandService,
) {
    @PatchMapping("/{notificationId}/read")
    fun markAsRead(
        @LoginMember memberInfo: MemberInfo,
        @PathVariable notificationId: Long,
    ) {
        notificationCommandService.markAsRead(notificationId, memberInfo.id)
    }

    @PatchMapping("/read-all")
    fun markAllAsRead(@LoginMember memberInfo: MemberInfo) {
        notificationCommandService.markAllAsRead(memberInfo.id)
    }
}
