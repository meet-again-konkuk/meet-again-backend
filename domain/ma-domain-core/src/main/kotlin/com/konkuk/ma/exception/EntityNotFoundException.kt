package com.konkuk.ma.exception

class EntityNotFoundException(
    entityType: EntityType,
    key: String,
    value: String,
) : BusinessException(
    message = "${entityType.entityName}을(를) 찾을 수 없습니다.",
    dataMessage = "$key: $value",
    logLevel = LogLevel.WARN,
)
