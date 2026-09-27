package com.konkuk.ma.domain.community.domain.image

class PostImage(
    val id: Long,
    val postId: Long,
    val storageKey: String,
    val originalFilename: String,
    val mimeType: String,
    val fileSize: Long,
    val thumbnailKey: String?,
)
