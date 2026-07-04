package com.konkuk.ma.domain.community.domain

class PostWithAuthor(
    val post: Post,
    val nickname: String,
    val likeCount: Int,
    val likedByMe: Boolean,
    val isMine: Boolean,
    val commentCount: Int,
)
