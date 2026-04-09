package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.common.domain.Email

class PostLike(
    val id: Long = 0L,
    val postId: Long,
    val memberEmail: Email,
)
