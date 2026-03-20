package com.konkuk.ma.domain.member.exception

import com.konkuk.ma.exception.BusinessException

class DuplicateEmailException(
    email: String
) : BusinessException(
    message = "이미 사용중인 이메일입니다.",
    dataMessage = "email: $email",
    logLevel = LogLevel.WARN
)
