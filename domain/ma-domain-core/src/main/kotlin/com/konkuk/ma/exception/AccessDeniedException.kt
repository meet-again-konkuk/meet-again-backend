package com.konkuk.ma.exception

class AccessDeniedException(
    entityType: EntityType,
    ownerInfo: String,
    requesterInfo: String,
) : BusinessException(
    message = "${entityType.entityName}에 대한 접근 권한이 없습니다.",
    dataMessage = "entityType: ${entityType.entityName}, owner: $ownerInfo, requester: $requesterInfo",
    logLevel = LogLevel.WARN,
)
