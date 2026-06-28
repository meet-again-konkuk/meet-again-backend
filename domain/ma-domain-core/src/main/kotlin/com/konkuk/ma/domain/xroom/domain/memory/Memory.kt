package com.konkuk.ma.domain.xroom.domain.memory

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

    fun sortKey(): LocalDate = eventDate.sortKey()
}
