package com.konkuk.ma.domain.community.domain

class CommentWithAuthor(
    val comment: Comment,
    val nickname: String,
    val likeCount: Int,
    val replies: List<ReplyWithAuthor> = emptyList(),
    val remainingReplyCount: Int = 0,
    val likedByMe: Boolean,
    val isMine: Boolean,
)
