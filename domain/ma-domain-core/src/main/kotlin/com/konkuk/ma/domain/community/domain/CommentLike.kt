package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.common.domain.Email

class CommentLike(
    val id: Long = 0L,
    val commentId: Long,
    val memberEmail: Email,
)
