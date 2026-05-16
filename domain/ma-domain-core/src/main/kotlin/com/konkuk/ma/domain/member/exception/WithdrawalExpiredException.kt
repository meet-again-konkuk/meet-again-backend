package com.konkuk.ma.domain.member.exception

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.exception.BusinessException

class WithdrawalExpiredException(
    email: Email
) : BusinessException(
    message = "탈퇴 유예 기간이 만료되었습니다.",
    dataMessage = "email: $email",
    logLevel = LogLevel.WARN
)
