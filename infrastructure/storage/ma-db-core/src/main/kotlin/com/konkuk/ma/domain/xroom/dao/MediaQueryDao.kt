package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.domain.xroom.entity.MediaEntity
import com.konkuk.ma.domain.xroom.entity.table.MemoryMediaTable
import org.springframework.stereotype.Component

@Component
class MediaQueryDao {
    fun findActiveByMemory(memoryId: Long): MediaEntity? {
        return MemoryMediaTable
            .activeRows { MemoryMediaTable.memoryId eq memoryId }
            .limit(1)
            .firstOrNull()
            ?.let { MediaEntity.from(it) }
    }

    fun findActiveByMemories(memoryIds: Set<Long>): List<MediaEntity> {
        if (memoryIds.isEmpty()) return emptyList()
        return MemoryMediaTable
            .activeRows { MemoryMediaTable.memoryId inList memoryIds }
            .map { MediaEntity.from(it) }
    }
}
