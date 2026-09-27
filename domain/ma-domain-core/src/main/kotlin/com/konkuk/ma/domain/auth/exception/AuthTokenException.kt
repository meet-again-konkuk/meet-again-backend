package com.konkuk.ma.domain.auth.exception

import com.konkuk.ma.exception.BusinessException

class AuthTokenException(
    token: String,

    val jwtExceptionType: JwtExceptionType,

    throwable: Throwable? = null,

    logLevel: LogLevel = LogLevel.INFO,
) : BusinessException(
    message = jwtExceptionType.message,
    dataMessage = "jwtExceptionType: ${jwtExceptionType.name}, token: $token",
    cause = throwable,
    logLevel = logLevel
) {
    fun isExpired(): Boolean {
        return jwtExceptionType.isExpired()
    }

    fun isMalformed(): Boolean {
        return jwtExceptionType.isMalformed()
    }

    fun isInvalid(): Boolean {
        return jwtExceptionType.isInvalid()
    }

    fun isOtherError(): Boolean {
        return jwtExceptionType.isOtherError()
    }
}
