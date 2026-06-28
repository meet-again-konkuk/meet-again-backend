package com.konkuk.ma.domain.xroom.fixture

import com.konkuk.ma.domain.xroom.domain.memory.EmotionTags
import com.konkuk.ma.domain.xroom.domain.memory.EventDate
import com.konkuk.ma.domain.xroom.domain.memory.EventDatePrecision
import com.konkuk.ma.domain.xroom.domain.memory.Memory
import com.konkuk.ma.domain.xroom.domain.memory.MemoryContent
import com.konkuk.ma.domain.xroom.domain.memory.MemoryTitle
import java.time.LocalDate
import java.time.LocalDateTime

object MemoryFixture {
    fun create(
        id: Long = 1L,
        xroomId: Long = 1L,
        title: MemoryTitle = MemoryTitle("첫 만남"),
        eventDate: EventDate = EventDate.of(EventDatePrecision.DAY, LocalDate.of(2019, 5, 10)),
        location: String? = "서울",
        emotionTags: EmotionTags = EmotionTags.of(listOf("설렘", "행복")),
        content: MemoryContent = MemoryContent.of(text = "그날의 기억", letter = null),
        createdDate: LocalDateTime = LocalDateTime.now(),
    ): Memory {
        return Memory(
            id = id,
            xroomId = xroomId,
            title = title,
            eventDate = eventDate,
            location = location,
            emotionTags = emotionTags,
            content = content,
            createdDate = createdDate,
        )
    }
}
