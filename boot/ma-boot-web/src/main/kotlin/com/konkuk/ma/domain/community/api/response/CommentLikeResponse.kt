package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.community.domain.CommentLikeResult

class CommentLikeResponse(
    val liked: Boolean,
    val likeCount: Int,
) {
    companion object {
        fun from(result: CommentLikeResult): CommentLikeResponse {
            return CommentLikeResponse(
                liked = result.liked,
                likeCount = result.likeCount,
            )
        }
    }
}
