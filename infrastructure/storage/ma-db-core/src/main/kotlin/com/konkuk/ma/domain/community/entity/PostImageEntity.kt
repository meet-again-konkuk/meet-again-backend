package com.konkuk.ma.domain.community.entity

import com.konkuk.ma.domain.community.domain.image.PostImage
import com.konkuk.ma.domain.community.entity.table.PostImageTable
import org.jetbrains.exposed.sql.ResultRow

class PostImageEntity(
    val id: Long,
    val postId: Long,
    val storageKey: String,
    val originalFilename: String,
    val mimeType: String,
    val fileSize: Long,
    val thumbnailKey: String?,
) {
    fun toDomain(): PostImage {
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
        fun from(row: ResultRow): PostImageEntity {
            return PostImageEntity(
                id = row[PostImageTable.id].value,
                postId = row[PostImageTable.postId],
                storageKey = row[PostImageTable.storageKey],
                originalFilename = row[PostImageTable.originalFilename],
                mimeType = row[PostImageTable.mimeType],
                fileSize = row[PostImageTable.fileSize],
                thumbnailKey = row[PostImageTable.thumbnailKey],
            )
        }
    }
}
