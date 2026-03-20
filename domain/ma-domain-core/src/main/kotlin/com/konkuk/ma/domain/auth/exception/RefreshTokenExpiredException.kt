package com.konkuk.ma.domain.auth.exception

import com.konkuk.ma.exception.BusinessException

class RefreshTokenExpiredException(
    email: String,
    expirationDate: String
) : BusinessException(
    message = "Refresh token이 만료되었습니다.",
    dataMessage = "email: $email, expired date: $expirationDate",
    logLevel = LogLevel.WARN
)
