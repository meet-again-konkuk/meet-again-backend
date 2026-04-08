package com.konkuk.ma.domain.community.exception

import com.konkuk.ma.exception.BusinessException

class ReplyDepthExceededException(
    parentCommentId: Long,
) : BusinessException(
    message = "대댓글에는 답글을 달 수 없습니다.",
    dataMessage = "parentCommentId: $parentCommentId",
    logLevel = LogLevel.WARN,
)
