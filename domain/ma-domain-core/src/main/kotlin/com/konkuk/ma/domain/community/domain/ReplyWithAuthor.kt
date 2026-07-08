package com.konkuk.ma.domain.community.domain

class ReplyWithAuthor(
    val comment: Comment,
    val nickname: String,
    val likeCount: Int,
    val likedByMe: Boolean,
    val isMine: Boolean,
    val blockedAuthor: Boolean = false,
) {
    fun displayContent(): String = comment.displayContent(blockedAuthor)
}
