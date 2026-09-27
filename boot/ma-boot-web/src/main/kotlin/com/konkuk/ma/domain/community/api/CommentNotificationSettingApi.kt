package com.konkuk.ma.domain.community.api

import com.konkuk.ma.domain.community.api.request.CommentNotificationSettingRequest
import com.konkuk.ma.domain.community.application.CommentNotificationSettingCommandService
import com.konkuk.ma.support.security.LoginMember
import com.konkuk.ma.support.security.MemberInfo
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/community/posts/{postId}/comment-notification")
class CommentNotificationSettingApi(
    private val commentNotificationSettingCommandService: CommentNotificationSettingCommandService,
) {
    @PatchMapping
    fun set(
        @LoginMember memberInfo: MemberInfo,
        @PathVariable postId: Long,
        @Valid @RequestBody request: CommentNotificationSettingRequest,
    ) {
        commentNotificationSettingCommandService.set(memberInfo.id, postId, request.enabled)
    }
}
