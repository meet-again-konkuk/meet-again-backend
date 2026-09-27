package com.konkuk.ma.domain.member.exception

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.exception.BusinessException

class AlreadyWithdrawalRequestedException(
    email: Email
) : BusinessException(
    message = "이미 탈퇴 신청 중인 회원입니다.",
    dataMessage = "email: $email",
    logLevel = LogLevel.WARN
)
