package com.konkuk.ma.domain.community.exception

import com.konkuk.ma.exception.BusinessException

class NotRootCommentException(
    commentId: Long,
) : BusinessException(
    message = "루트 댓글만 조회할 수 있습니다.",
    dataMessage = "commentId: $commentId",
    logLevel = LogLevel.WARN,
)
