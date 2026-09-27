package com.konkuk.ma.domain.xroom.fixture

import com.konkuk.ma.domain.xroom.domain.memory.MemoryDetails

object MemoryDetailsFixture {
    fun create(
        title: String = "첫 만남",
        eventDate: String = "2019-05-10",
        eventDatePrecision: String = "DAY",
        location: String? = "서울",
        emotionTags: List<String> = listOf("설렘", "행복"),
        text: String? = "그날의 기억",
        letter: String? = null,
    ): MemoryDetails {
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
}
