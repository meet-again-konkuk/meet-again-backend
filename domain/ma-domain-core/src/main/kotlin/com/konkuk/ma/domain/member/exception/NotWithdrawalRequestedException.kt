package com.konkuk.ma.domain.member.exception

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.exception.BusinessException

class NotWithdrawalRequestedException(
    email: Email
) : BusinessException(
    message = "탈퇴 신청 상태가 아닌 회원입니다.",
    dataMessage = "email: $email",
    logLevel = LogLevel.WARN
)
