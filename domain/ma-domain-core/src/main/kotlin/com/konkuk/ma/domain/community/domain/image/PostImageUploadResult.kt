package com.konkuk.ma.domain.community.domain.image

class PostImageUploadResult(
    val mediaId: Long,
    val postId: Long,
    val imageUrl: String,
    val thumbnailUrl: String?,
)
