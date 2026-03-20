package com.konkuk.ma.domain.member.exception

import com.konkuk.ma.exception.BusinessException

class SmsNotVerifiedException(
    phoneNumber: String
) : BusinessException(
    message = "휴대폰 번호 인증이 완료되지 않았습니다.",
    dataMessage = "phoneNumber: $phoneNumber",
    logLevel = LogLevel.WARN
)
