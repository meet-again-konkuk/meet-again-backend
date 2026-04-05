package com.konkuk.ma.domain.matching.exception

import com.konkuk.ma.exception.BusinessException

class MatchingTargetNotFoundException(
    targetEmail: String
) : BusinessException(
    message = "매칭 상대방의 회원 정보를 찾을 수 없습니다.",
    dataMessage = "targetEmail: $targetEmail",
    logLevel = LogLevel.ERROR
)
