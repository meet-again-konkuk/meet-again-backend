package com.konkuk.ma.domain.xroom.fixture

import com.konkuk.ma.domain.xroom.domain.media.Media

object MediaFixture {
    fun create(
        id: Long = 1L,
        memoryId: Long = 1L,
        storageKey: String = "memory/memory-photo/1/photo.jpg",
        originalFilename: String = "photo.jpg",
        mimeType: String = "image/jpeg",
        fileSize: Long = 1024L,
        thumbnailKey: String? = "memory/thumbnail/1/thumb_photo.jpg",
    ): Media {
        return Media(
            id = id,
            memoryId = memoryId,
            storageKey = storageKey,
            originalFilename = originalFilename,
            mimeType = mimeType,
            fileSize = fileSize,
            thumbnailKey = thumbnailKey,
        )
    }
}
