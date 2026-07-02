package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.domain.xroom.domain.media.NewMedia
import com.konkuk.ma.domain.xroom.entity.table.MemoryMediaTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.stereotype.Component

@Component
class MediaCommandDao {
    fun save(newMedia: NewMedia): Long {
        return MemoryMediaTable.insertAndGetId {
            it[memoryId] = newMedia.memoryId
            it[storageKey] = newMedia.storageKey
            it[originalFilename] = newMedia.originalFilename
            it[mimeType] = newMedia.mimeType
            it[fileSize] = newMedia.fileSize
            it[thumbnailKey] = newMedia.thumbnailKey
        }.value
    }

    fun softDeleteByMemory(memoryId: Long, memberId: Long) {
        MemoryMediaTable.softDelete({ MemoryMediaTable.memoryId eq memoryId }, memberId.toString())
    }

    fun softDeleteByMemories(memoryIds: Set<Long>, memberId: Long) {
        if (memoryIds.isEmpty()) return
        MemoryMediaTable.softDelete({ MemoryMediaTable.memoryId inList memoryIds }, memberId.toString())
    }

    fun delete(mediaId: Long) {
        MemoryMediaTable.deleteWhere { MemoryMediaTable.id eq mediaId }
    }
}
