package com.konkuk.ma.domain.xroom.domain.memory

import com.konkuk.ma.exception.InvalidValueException

class NewMemory(
    val xroomId: Long,
    title: String,
    eventDate: String,
    eventDatePrecision: String,
    location: String?,
    emotionTags: List<String>,
    text: String?,
    letter: String?,
) {
    val title: MemoryTitle = MemoryTitle(title)
    val eventDate: EventDate = EventDate.parse(eventDate, EventDatePrecision.from(eventDatePrecision))
    val location: String? = normalizeLocation(location)
    val emotionTags: EmotionTags = EmotionTags.of(emotionTags)
    val content: MemoryContent = MemoryContent.of(text, letter)

    private fun normalizeLocation(location: String?): String? {
        val normalized = location?.takeIf { it.isNotBlank() } ?: return null
        if (normalized.length > MAX_LOCATION_LENGTH) {
            throw InvalidValueException(NewMemory::class, normalized, "장소는 ${MAX_LOCATION_LENGTH}자 이하여야 합니다.")
        }
        return normalized
    }

    companion object {
        private const val MAX_LOCATION_LENGTH = 200
    }
}
