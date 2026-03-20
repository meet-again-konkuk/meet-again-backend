package com.konkuk.ma.domain.member.exception

import com.konkuk.ma.exception.BusinessException

class PasswordMismatchException(
    email: String
) : BusinessException(
    message = "비밀번호가 올바르지 않습니다.",
    dataMessage = "email: $email",
    logLevel = LogLevel.WARN
)
