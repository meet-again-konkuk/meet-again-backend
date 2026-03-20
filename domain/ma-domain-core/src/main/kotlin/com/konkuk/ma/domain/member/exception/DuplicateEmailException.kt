package com.konkuk.ma.domain.member.exception

import com.konkuk.ma.exception.BusinessException

class DuplicateEmailException(
    email: String
) : BusinessException(
    message = MESSAGE,
    dataMessage = "email: $email",
    logLevel = LogLevel.WARN
) {
    companion object {
        const val MESSAGE = "이미 사용중인 이메일입니다."
    }
}
