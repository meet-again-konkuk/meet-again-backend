package com.konkuk.ma.domain.community.domain

class PostDetail(
    val post: Post,
    val nickname: String,
    val likeCount: Int,
    val comments: List<CommentWithAuthor>,
)
