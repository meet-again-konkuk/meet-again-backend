package com.konkuk.ma.exception

class DuplicateException(
    entityType: EntityType,
    field: String,
    value: String,
) : BusinessException(
    message = "이미 사용중인 ${field}입니다.",
    dataMessage = "entityType: ${entityType.entityName}, $field: $value",
    logLevel = LogLevel.WARN,
)
