package com.konkuk.ma.domain.xroom.api.request

import com.konkuk.ma.domain.xroom.domain.memory.MemoryDetails
import com.konkuk.ma.domain.xroom.domain.memory.NewMemory

class AddMemoryRequest(
    val title: String,
    val eventDate: String,
    val eventDatePrecision: String,
    val location: String? = null,
    val emotionTags: List<String> = emptyList(),
    val text: String? = null,
    val letter: String? = null,
) {
    fun toMemoryDetails(): MemoryDetails {
        return MemoryDetails.of(
            title = title,
            eventDate = eventDate,
            eventDatePrecision = eventDatePrecision,
            location = location,
            emotionTags = emotionTags,
            text = text,
            letter = letter,
        )
    }

    fun toNewMemory(xroomId: Long): NewMemory {
        return NewMemory(xroomId = xroomId, details = toMemoryDetails())
    }
}
