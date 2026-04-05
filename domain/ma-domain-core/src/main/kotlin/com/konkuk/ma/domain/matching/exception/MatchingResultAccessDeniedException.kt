package com.konkuk.ma.domain.matching.exception

import com.konkuk.ma.exception.BusinessException

class MatchingResultAccessDeniedException(
    matchingResultId: Long,
    email: String,
) : BusinessException(
    message = "매칭 결과에 대한 접근 권한이 없습니다.",
    dataMessage = "matchingResultId: $matchingResultId, requestEmail: $email",
    logLevel = LogLevel.WARN,
)
