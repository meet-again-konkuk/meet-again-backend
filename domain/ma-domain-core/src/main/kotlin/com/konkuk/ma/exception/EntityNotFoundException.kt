package com.konkuk.ma.exception

class EntityNotFoundException(
    entityType: EntityType,
    value: String,
) : BusinessException(
    message = "${entityType.entityName}을(를) 찾을 수 없습니다.",
    dataMessage = "${entityType.keyName}: $value",
    logLevel = LogLevel.WARN,
)
