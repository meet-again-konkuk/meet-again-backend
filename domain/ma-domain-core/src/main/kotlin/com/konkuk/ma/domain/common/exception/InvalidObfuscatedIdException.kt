package com.konkuk.ma.domain.common.exception

import com.konkuk.ma.exception.BusinessException

class InvalidObfuscatedIdException(
    encodedValue: String
) : BusinessException(
    message = "유효하지 않은 ID입니다.",
    dataMessage = "encoded: $encodedValue",
    logLevel = LogLevel.WARN
)
