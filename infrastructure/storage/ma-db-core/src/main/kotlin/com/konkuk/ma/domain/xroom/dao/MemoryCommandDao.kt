package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.domain.xroom.domain.memory.NewMemory
import com.konkuk.ma.domain.xroom.entity.table.MemoryEmotionTagTable
import com.konkuk.ma.domain.xroom.entity.table.MemoryTable
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.stereotype.Component

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

    private fun saveEmotionTags(memoryId: Long, tags: List<String>) {
        if (tags.isEmpty()) return
        MemoryEmotionTagTable.batchInsert(tags) {
            this[MemoryEmotionTagTable.memoryId] = memoryId
            this[MemoryEmotionTagTable.tag] = it
        }
    }
}
