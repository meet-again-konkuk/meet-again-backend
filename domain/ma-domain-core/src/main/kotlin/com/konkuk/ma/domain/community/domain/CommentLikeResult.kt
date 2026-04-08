package com.konkuk.ma.domain.community.domain

class CommentLikeResult(
    val liked: Boolean,
    val likeCount: Int,
) {
    companion object {
        fun liked(likeCount: Int): CommentLikeResult = CommentLikeResult(liked = true, likeCount = likeCount)
        fun unliked(likeCount: Int): CommentLikeResult = CommentLikeResult(liked = false, likeCount = likeCount)
    }
}
