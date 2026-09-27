package com.konkuk.ma.domain.xroom.domain.memory

import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.EntityType
import java.time.LocalDate
import java.time.LocalDateTime

class Memory(
    val id: Long,
    val xroomId: Long,
    val title: MemoryTitle,
    val eventDate: EventDate,
    val location: String?,
    val emotionTags: EmotionTags,
    val content: MemoryContent,
    val createdDate: LocalDateTime,
) {
    val titleValue: String get() = title.value
    val eventDateWire: String get() = eventDate.toWire()
    val eventDatePrecisionValue: String get() = eventDate.precisionName()
    val emotionTagValues: List<String> get() = emotionTags.data
    val text: String? get() = content.text
    val letter: String? get() = content.letter

    fun sortKey(): LocalDate = eventDate.normalizedDate

    fun update(details: MemoryDetails): Memory {
        return Memory(
            id = id,
            xroomId = xroomId,
            title = details.title,
            eventDate = details.eventDate,
            location = details.location,
            emotionTags = details.emotionTags,
            content = details.content,
            createdDate = createdDate,
        )
    }

    fun validateBelongsTo(xroomId: Long) {
        if (this.xroomId != xroomId) {
            throw AccessDeniedException(EntityType.MEMORY, this.xroomId.toString(), xroomId.toString())
        }
    }
}
