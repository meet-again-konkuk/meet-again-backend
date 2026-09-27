package com.konkuk.ma.domain.xroom.entity

import com.konkuk.ma.domain.xroom.domain.media.Media
import com.konkuk.ma.domain.xroom.entity.table.MemoryMediaTable
import org.jetbrains.exposed.sql.ResultRow

class MediaEntity(
    val id: Long,
    val memoryId: Long,
    val storageKey: String,
    val originalFilename: String,
    val mimeType: String,
    val fileSize: Long,
    val thumbnailKey: String?,
) {
    fun toDomain(): Media {
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
        fun from(row: ResultRow): MediaEntity {
            return MediaEntity(
                id = row[MemoryMediaTable.id].value,
                memoryId = row[MemoryMediaTable.memoryId],
                storageKey = row[MemoryMediaTable.storageKey],
                originalFilename = row[MemoryMediaTable.originalFilename],
                mimeType = row[MemoryMediaTable.mimeType],
                fileSize = row[MemoryMediaTable.fileSize],
                thumbnailKey = row[MemoryMediaTable.thumbnailKey],
            )
        }
    }
}
