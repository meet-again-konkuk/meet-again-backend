package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.community.domain.PostLikeResult

class PostLikeResponse(
    val liked: Boolean,
    val likeCount: Int,
) {
    companion object {
        fun from(result: PostLikeResult): PostLikeResponse {
            return PostLikeResponse(
                liked = result.liked,
                likeCount = result.likeCount,
            )
        }
    }
}
