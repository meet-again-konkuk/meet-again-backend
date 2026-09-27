package com.konkuk.ma.domain.community.domain

class PostDetail(
    val post: Post,
    val nickname: String,
    val likeCount: Int,
    val comments: List<CommentWithAuthor>,
    val likedByMe: Boolean,
    val isMine: Boolean,
    val imageUrl: String? = null,
    val thumbnailUrl: String? = null,
)
