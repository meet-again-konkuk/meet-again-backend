package com.konkuk.ma.domain.auth.exception

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.exception.BusinessException

class PasswordMismatchException(
    email: Email
) : BusinessException(
    message = "비밀번호가 올바르지 않습니다.",
    dataMessage = "email: $email",
    logLevel = LogLevel.WARN
)
