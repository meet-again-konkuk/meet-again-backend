package com.konkuk.ma.domain.xroom.domain.media

import com.konkuk.ma.domain.common.domain.file.PhotoFile

class NewMedia(
    val memoryId: Long,
    val storageKey: String,
    val originalFilename: String,
    val mimeType: String,
    val fileSize: Long,
    val thumbnailKey: String?,
) {
    fun toMedia(id: Long): Media {
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

    companion object {
        fun of(memoryId: Long, photoFile: PhotoFile, storageKey: String, thumbnailKey: String?): NewMedia {
            return NewMedia(
                memoryId = memoryId,
                storageKey = storageKey,
                originalFilename = photoFile.originalFileName,
                mimeType = photoFile.mimeType,
                fileSize = photoFile.sizeInBytes,
                thumbnailKey = thumbnailKey,
            )
        }
    }
}
