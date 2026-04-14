package com.konkuk.ma.exception

class InvalidObfuscatedIdException(
    encodedValue: String
) : BusinessException(
    message = "유효하지 않은 ID입니다.",
    dataMessage = "encoded: $encodedValue",
    logLevel = LogLevel.WARN
)
