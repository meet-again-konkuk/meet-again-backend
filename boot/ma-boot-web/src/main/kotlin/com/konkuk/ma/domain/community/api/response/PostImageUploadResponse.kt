package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.community.domain.image.PostImageUploadResult

class PostImageUploadResponse(
    val mediaId: Long,
    val postId: Long,
    val imageUrl: String,
    val thumbnailUrl: String?,
) {
    companion object {
        fun from(result: PostImageUploadResult): PostImageUploadResponse {
            return PostImageUploadResponse(
                mediaId = result.mediaId,
                postId = result.postId,
                imageUrl = result.imageUrl,
                thumbnailUrl = result.thumbnailUrl,
            )
        }
    }
}
