package com.konkuk.ma.domain.xroom.domain.memory

class NewMemory(
    val xroomId: Long,
    details: MemoryDetails,
) {
    val title: MemoryTitle = details.title
    val eventDate: EventDate = details.eventDate
    val location: String? = details.location
    val emotionTags: EmotionTags = details.emotionTags
    val content: MemoryContent = details.content
}
