package com.konkuk.ma.domain.community.api.request

class CommentNotificationSettingRequest(
    // 외부 제약: Jackson은 FAIL_ON_NULL_FOR_PRIMITIVES 설정 시 Boolean의 null·누락 입력을 역직렬화 오류로 처리한다.
    val enabled: Boolean,
)
