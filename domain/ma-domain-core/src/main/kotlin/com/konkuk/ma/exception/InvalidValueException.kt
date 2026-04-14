package com.konkuk.ma.exception

import kotlin.reflect.KClass

class InvalidValueException(
    type: KClass<*>,
    value: Any,
    reason: String
) : BusinessException(
    message = "유효하지 않은 값입니다.",
    dataMessage = "${type.simpleName}: $value ($reason)",
    logLevel = LogLevel.WARN
)
