package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.domain.xroom.domain.memory.Memory
import com.konkuk.ma.domain.xroom.domain.memory.NewMemory
import com.konkuk.ma.domain.xroom.entity.table.MemoryEmotionTagTable
import com.konkuk.ma.domain.xroom.entity.table.MemoryTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class MemoryCommandDao {
    fun save(newMemory: NewMemory): Long {
        val memoryId = MemoryTable.insertAndGetId {
            it[xroomId] = newMemory.xroomId
            it[title] = newMemory.title.value
            it[eventDate] = newMemory.eventDate.normalizedDate
            it[eventDatePrecision] = newMemory.eventDate.precisionName()
            it[location] = newMemory.location
            it[text] = newMemory.content.text
            it[letter] = newMemory.content.letter
        }.value
        saveEmotionTags(memoryId, newMemory.emotionTags.data)
        return memoryId
    }

    fun update(memory: Memory, memberId: Long) {
        MemoryTable.update({ MemoryTable.id eq memory.id }) {
            it[title] = memory.titleValue
            it[eventDate] = memory.eventDate.normalizedDate
            it[eventDatePrecision] = memory.eventDatePrecisionValue
            it[location] = memory.location
            it[text] = memory.text
            it[letter] = memory.letter
            it[lastModifiedDate] = LocalDateTime.now()
            it[lastModifiedBy] = memberId.toString()
        }
        replaceEmotionTags(memory.id, memory.emotionTagValues)
    }

    fun delete(memoryId: Long, memberId: Long) {
        MemoryTable.softDelete({ MemoryTable.id eq memoryId }, memberId.toString())
    }

    private fun saveEmotionTags(memoryId: Long, tags: List<String>) {
        if (tags.isEmpty()) return
        MemoryEmotionTagTable.batchInsert(tags) {
            this[MemoryEmotionTagTable.memoryId] = memoryId
            this[MemoryEmotionTagTable.tag] = it
        }
    }

    private fun replaceEmotionTags(memoryId: Long, tags: List<String>) {
        MemoryEmotionTagTable.deleteWhere { MemoryEmotionTagTable.memoryId eq memoryId }
        saveEmotionTags(memoryId, tags)
    }
}
