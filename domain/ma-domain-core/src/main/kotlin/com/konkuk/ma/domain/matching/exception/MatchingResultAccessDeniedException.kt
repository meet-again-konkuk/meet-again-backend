package com.konkuk.ma.domain.matching.exception

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.exception.BusinessException

class MatchingResultAccessDeniedException(
    matchingResultId: Long,
    ownerEmail: Email,
    requestEmail: Email,
) : BusinessException(
    message = "매칭 결과에 대한 접근 권한이 없습니다.",
    dataMessage = "matchingResultId: $matchingResultId, ownerEmail: $ownerEmail, requestEmail: $requestEmail",
    logLevel = LogLevel.WARN,
)
