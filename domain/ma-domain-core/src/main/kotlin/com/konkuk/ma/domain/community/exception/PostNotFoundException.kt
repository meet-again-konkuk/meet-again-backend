package com.konkuk.ma.domain.community.exception

import com.konkuk.ma.exception.BusinessException

class PostNotFoundException(
    postId: Long,
) : BusinessException(
    message = "존재하지 않는 게시글에는 댓글을 달 수 없습니다.",
    dataMessage = "postId: $postId",
    logLevel = LogLevel.WARN,
)
