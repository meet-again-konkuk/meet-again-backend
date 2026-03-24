package com.konkuk.ma.domain.member.domain.photo

import com.konkuk.ma.domain.common.domain.EnumWithDisplayName

enum class ApprovalStatus(override val displayName: String) : EnumWithDisplayName {
    PENDING("대기"),
    APPROVED("승인"),
    REJECTED("거절");

    fun isPending(): Boolean = this == PENDING
}
