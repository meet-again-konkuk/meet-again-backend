package com.konkuk.ma.exception

import kotlin.reflect.KFunction

class AuthTokenException(
    token: String,

    val jwtExceptionType: JwtExceptionType,

    throwable: Throwable? = null,

    logLevel: LogLevel = LogLevel.INFO,

    callerFunction: KFunction<*>? = null
) : BusinessException(
    message = jwtExceptionType.message,
    dataMessage = "token : $token",
    callerFunction = callerFunction,
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
