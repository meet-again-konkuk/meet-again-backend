package com.konkuk.ma.domain.auth.exception

enum class JwtExceptionType(val message: String) {
    EXPIRED("만료된 토큰입니다."),
    MALFORMED("잘못된 형식의 토큰입니다."),
    INVALID("유효하지 않은 토큰입니다."),
    UNSUPPORTED("지원하지 않는 형식의 토큰입니다."),
    SIGNATURE("유효하지 않은 Signature입니다."),
    ILLEGAL_ARGUMENT("잘못된 인자입니다."),
    ETC("유효하지 않은 JWT입니다.");

    fun isExpired(): Boolean {
        return this == EXPIRED
    }

    fun isMalformed(): Boolean {
        return this == MALFORMED
    }

    fun isInvalid(): Boolean {
        return this == INVALID
    }

    fun isOtherError(): Boolean {
        return this == UNSUPPORTED || this == SIGNATURE || this == ILLEGAL_ARGUMENT || this == ETC
    }
}
