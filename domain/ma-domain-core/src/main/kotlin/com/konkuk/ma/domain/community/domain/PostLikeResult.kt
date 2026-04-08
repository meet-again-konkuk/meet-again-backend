package com.konkuk.ma.domain.community.domain

class PostLikeResult(
    val liked: Boolean,
    val likeCount: Int,
) {
    companion object {
        fun liked(likeCount: Int): PostLikeResult = PostLikeResult(liked = true, likeCount = likeCount)
        fun unliked(likeCount: Int): PostLikeResult = PostLikeResult(liked = false, likeCount = likeCount)
    }
}
