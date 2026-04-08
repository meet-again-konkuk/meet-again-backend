package com.konkuk.ma.domain.community.domain

class CommentLike(
    val id: Long = 0L,
    val commentId: Long,
    val memberEmail: String,
)
