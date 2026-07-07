package com.konkuk.ma.domain.community.fixture

import com.konkuk.ma.domain.community.domain.image.PostImage

object PostImageFixture {
    fun create(
        id: Long = 1L,
        postId: Long = 1L,
        storageKey: String = "community/post-image/1/photo.jpg",
        originalFilename: String = "photo.jpg",
        mimeType: String = "image/jpeg",
        fileSize: Long = 1024L,
        thumbnailKey: String? = "community/thumbnail/1/thumb_photo.jpg",
    ): PostImage {
        return PostImage(
            id = id,
            postId = postId,
            storageKey = storageKey,
            originalFilename = originalFilename,
            mimeType = mimeType,
            fileSize = fileSize,
            thumbnailKey = thumbnailKey,
        )
    }
}
