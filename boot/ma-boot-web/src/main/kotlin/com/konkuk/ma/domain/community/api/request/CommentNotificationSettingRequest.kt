package com.konkuk.ma.domain.community.api.request

class CommentNotificationSettingRequest(
    // null/누락은 fail-on-null-for-primitives + MissingKotlinParameter로 역직렬화 단계에서 400 처리
    val enabled: Boolean,
)
