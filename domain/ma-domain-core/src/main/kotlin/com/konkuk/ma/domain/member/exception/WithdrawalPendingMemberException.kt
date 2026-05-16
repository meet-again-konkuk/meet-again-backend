package com.konkuk.ma.domain.member.exception

import com.konkuk.ma.exception.BusinessException

class WithdrawalPendingMemberException(
    field: String,
    value: String
) : BusinessException(
    message = "탈퇴 진행 중인 계정이 있습니다.",
    dataMessage = "$field: $value",
    logLevel = LogLevel.WARN
)
