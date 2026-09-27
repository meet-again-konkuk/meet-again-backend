package com.konkuk.ma.domain.xroom.api.response

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.xroom.domain.memory.Memory
import com.konkuk.ma.support.id.EncryptId

class MemoryDetailResponse(
    @EncryptId(ObfuscationType.MEMORY)
    val id: Long,
    val title: String,
    val eventDate: String,
    val eventDatePrecision: String,
    val location: String?,
    val emotionTags: List<String>,
    val text: String?,
    val letter: String?,
    val photoUrl: String?,
) {
    companion object {
        fun from(memory: Memory, photoUrl: String?): MemoryDetailResponse {
            return MemoryDetailResponse(
                id = memory.id,
                title = memory.titleValue,
                eventDate = memory.eventDateWire,
                eventDatePrecision = memory.eventDatePrecisionValue,
                location = memory.location,
                emotionTags = memory.emotionTagValues,
                text = memory.text,
                letter = memory.letter,
                photoUrl = photoUrl,
            )
        }
    }
}
