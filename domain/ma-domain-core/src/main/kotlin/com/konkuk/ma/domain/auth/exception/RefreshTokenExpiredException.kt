package com.konkuk.ma.domain.auth.exception

import com.konkuk.ma.exception.BusinessException

class RefreshTokenExpiredException(
    memberId: Long,
    expirationDate: String
) : BusinessException(
    message = "Refresh token이 만료되었습니다.",
    dataMessage = "memberId: $memberId, expired date: $expirationDate",
    logLevel = LogLevel.WARN
)
