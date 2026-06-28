package com.konkuk.ma.domain.xroom.api.request

import com.konkuk.ma.domain.xroom.application.command.UpdateMemoryCommand

class UpdateMemoryRequest(
    val title: String,
    val eventDate: String,
    val eventDatePrecision: String,
    val location: String? = null,
    val emotionTags: List<String> = emptyList(),
    val text: String? = null,
    val letter: String? = null,
) {
    fun toCommand(): UpdateMemoryCommand {
        return UpdateMemoryCommand(
            title = title,
            eventDate = eventDate,
            eventDatePrecision = eventDatePrecision,
            location = location,
            emotionTags = emotionTags,
            text = text,
            letter = letter,
        )
    }
}
