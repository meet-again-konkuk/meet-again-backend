package com.konkuk.ma.domain.xroom.domain.media

class NewMedia(
    val memoryId: Long,
    val storageKey: String,
    val originalFilename: String,
    val mimeType: String,
    val fileSize: Long,
    val thumbnailKey: String?,
) {
    companion object {
        fun create(
            memoryId: Long,
            storageKey: String,
            originalFilename: String,
            mimeType: String,
            fileSize: Long,
            thumbnailKey: String?,
        ): NewMedia {
            return NewMedia(
                memoryId = memoryId,
                storageKey = storageKey,
                originalFilename = originalFilename,
                mimeType = mimeType,
                fileSize = fileSize,
                thumbnailKey = thumbnailKey,
            )
        }
    }
}
