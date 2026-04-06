package com.konkuk.ma.domain.matching.exception

import com.konkuk.ma.exception.BusinessException

class MatchingResultAccessDeniedException(
    matchingResultId: Long,
    ownerEmail: String,
    requestEmail: String,
) : BusinessException(
    message = "매칭 결과에 대한 접근 권한이 없습니다.",
    dataMessage = "matchingResultId: $matchingResultId, ownerEmail: $ownerEmail, requestEmail: $requestEmail",
    logLevel = LogLevel.WARN,
)
