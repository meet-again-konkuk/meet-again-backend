package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.domain.xroom.entity.MediaEntity
import com.konkuk.ma.domain.xroom.entity.table.MemoryMediaTable
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class MediaQueryDao {
    fun findActiveByMemories(memoryIds: Set<Long>): List<MediaEntity> {
        if (memoryIds.isEmpty()) return emptyList()
        return MemoryMediaTable
            .activeRows { MemoryMediaTable.memoryId inList memoryIds }
            .map { MediaEntity.from(it) }
    }

    fun findDeletedBefore(cutoff: LocalDateTime, cursorId: Long?, pageSize: Int): List<MediaEntity> {
        return MemoryMediaTable
            .selectAll()
            .where {
                val baseCondition = (MemoryMediaTable.deleted eq true) and (MemoryMediaTable.deletedDate less cutoff)
                if (cursorId == null) baseCondition else baseCondition and (MemoryMediaTable.id greater cursorId)
            }
            .orderBy(MemoryMediaTable.id to SortOrder.ASC)
            .limit(pageSize)
            .map { MediaEntity.from(it) }
    }
}
