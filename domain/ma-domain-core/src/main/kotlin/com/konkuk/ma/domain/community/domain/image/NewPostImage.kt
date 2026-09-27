package com.konkuk.ma.domain.community.domain.image

import com.konkuk.ma.domain.common.domain.file.PhotoFile

class NewPostImage(
    val postId: Long,
    val storageKey: String,
    val originalFilename: String,
    val mimeType: String,
    val fileSize: Long,
    val thumbnailKey: String?,
) {
    fun toPostImage(id: Long): PostImage {
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

    companion object {
        fun of(postId: Long, photoFile: PhotoFile, storageKey: String, thumbnailKey: String?): NewPostImage {
            return NewPostImage(
                postId = postId,
                storageKey = storageKey,
                originalFilename = photoFile.originalFileName,
                mimeType = photoFile.mimeType,
                fileSize = photoFile.sizeInBytes,
                thumbnailKey = thumbnailKey,
            )
        }
    }
}
