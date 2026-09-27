package com.konkuk.ma.domain.xroom.domain.memory

import com.konkuk.ma.exception.InvalidValueException

class MemoryDetails private constructor(
    val title: MemoryTitle,
    val eventDate: EventDate,
    val location: String?,
    val emotionTags: EmotionTags,
    val content: MemoryContent,
) {
    companion object {
        private const val MAX_LOCATION_LENGTH = 200

        fun of(
            title: String,
            eventDate: String,
            eventDatePrecision: String,
            location: String?,
            emotionTags: List<String>,
            text: String?,
            letter: String?,
        ): MemoryDetails {
            return MemoryDetails(
                title = MemoryTitle(title),
                eventDate = EventDate.parse(eventDate, EventDatePrecision.from(eventDatePrecision)),
                location = normalizeLocation(location),
                emotionTags = EmotionTags.of(emotionTags),
                content = MemoryContent.of(text, letter),
            )
        }

        private fun normalizeLocation(location: String?): String? {
            val normalized = location?.takeIf { it.isNotBlank() } ?: return null
            if (normalized.length > MAX_LOCATION_LENGTH) {
                throw InvalidValueException(MemoryDetails::class, normalized, "장소는 ${MAX_LOCATION_LENGTH}자 이하여야 합니다.")
            }
            return normalized
        }
    }
}
