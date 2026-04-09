package com.konkuk.ma.domain.community.exception

import com.konkuk.ma.exception.BusinessException

class CommentAccessDeniedException(
    commentId: Long,
    ownerEmail: String,
    requestEmail: String,
) : BusinessException(
    message = "댓글에 대한 접근 권한이 없습니다.",
    dataMessage = "commentId: $commentId, ownerEmail: $ownerEmail, requestEmail: $requestEmail",
    logLevel = LogLevel.WARN,
)
