package com.konkuk.ma.domain.xroom.entity

import com.konkuk.ma.domain.xroom.domain.memory.EmotionTags
import com.konkuk.ma.domain.xroom.domain.memory.EventDate
import com.konkuk.ma.domain.xroom.domain.memory.EventDatePrecision
import com.konkuk.ma.domain.xroom.domain.memory.Memory
import com.konkuk.ma.domain.xroom.domain.memory.MemoryContent
import com.konkuk.ma.domain.xroom.domain.memory.MemoryTitle
import com.konkuk.ma.domain.xroom.entity.table.MemoryTable
import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDate
import java.time.LocalDateTime

class MemoryEntity(
    val id: Long,
    val xroomId: Long,
    val title: String,
    val eventDate: LocalDate,
    val eventDatePrecision: String,
    val location: String?,
    val text: String?,
    val letter: String?,
    val emotionTags: List<String>,
    val createdDate: LocalDateTime,
) {
    fun toDomain(): Memory {
        return Memory(
            id = id,
            xroomId = xroomId,
            title = MemoryTitle(title),
            eventDate = EventDate.of(EventDatePrecision.from(eventDatePrecision), eventDate),
            location = location,
            emotionTags = EmotionTags.of(emotionTags),
            content = MemoryContent.of(text, letter),
            createdDate = createdDate,
        )
    }

    companion object {
        fun from(row: ResultRow, emotionTags: List<String>): MemoryEntity {
            return MemoryEntity(
                id = row[MemoryTable.id].value,
                xroomId = row[MemoryTable.xroomId],
                title = row[MemoryTable.title],
                eventDate = row[MemoryTable.eventDate],
                eventDatePrecision = row[MemoryTable.eventDatePrecision],
                location = row[MemoryTable.location],
                text = row[MemoryTable.text],
                letter = row[MemoryTable.letter],
                emotionTags = emotionTags,
                createdDate = row[MemoryTable.createdDate],
            )
        }
    }
}
