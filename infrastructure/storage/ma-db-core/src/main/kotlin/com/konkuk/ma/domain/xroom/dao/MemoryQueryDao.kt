package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.domain.xroom.entity.MemoryEntity
import com.konkuk.ma.domain.xroom.entity.table.MemoryEmotionTagTable
import com.konkuk.ma.domain.xroom.entity.table.MemoryTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.count
import org.springframework.stereotype.Component

@Component
class MemoryQueryDao {
    fun find(xroomId: Long): List<MemoryEntity> {
        val rows = MemoryTable
            .activeRows { MemoryTable.xroomId eq xroomId }
            .orderBy(MemoryTable.eventDate to SortOrder.ASC, MemoryTable.id to SortOrder.ASC)
            .toList()
        val memoryIds = rows.map { it[MemoryTable.id].value }.toSet()
        val tags = findTags(memoryIds)
        return rows.map { MemoryEntity.from(it, tags[it[MemoryTable.id].value] ?: emptyList()) }
    }

    fun findOne(memoryId: Long): MemoryEntity? {
        val row = MemoryTable
            .activeRows { MemoryTable.id eq memoryId }
            .limit(1)
            .firstOrNull()
            ?: return null
        val tags = findTags(setOf(memoryId))
        return MemoryEntity.from(row, tags[memoryId] ?: emptyList())
    }

    fun count(xroomIds: Set<Long>): Map<Long, Int> {
        if (xroomIds.isEmpty()) return emptyMap()
        val memoryCount = MemoryTable.id.count()
        return MemoryTable
            .select(MemoryTable.xroomId, memoryCount)
            .where { (MemoryTable.deleted eq false) and (MemoryTable.xroomId inList xroomIds) }
            .groupBy(MemoryTable.xroomId)
            .associate { it[MemoryTable.xroomId] to it[memoryCount].toInt() }
    }

    private fun findTags(memoryIds: Set<Long>): Map<Long, List<String>> {
        if (memoryIds.isEmpty()) return emptyMap()
        return MemoryEmotionTagTable
            .activeRows { MemoryEmotionTagTable.memoryId inList memoryIds }
            .groupBy({ it[MemoryEmotionTagTable.memoryId] }, { it[MemoryEmotionTagTable.tag] })
    }
}
