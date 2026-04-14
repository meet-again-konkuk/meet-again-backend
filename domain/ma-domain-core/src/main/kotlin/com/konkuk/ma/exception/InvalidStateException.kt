package com.konkuk.ma.exception

import kotlin.reflect.KClass

class InvalidStateException(
    type: KClass<*>,
    value: Any,
    reason: String,
) : BusinessException(
    message = "허용되지 않는 상태입니다.",
    dataMessage = "${type.simpleName}: $value ($reason)",
    logLevel = LogLevel.WARN,
)
