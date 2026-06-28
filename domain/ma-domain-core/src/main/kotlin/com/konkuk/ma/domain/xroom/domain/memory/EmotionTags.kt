package com.konkuk.ma.domain.xroom.domain.memory

import com.konkuk.ma.exception.InvalidValueException

class EmotionTags private constructor(
    val data: List<String>,
) {
    companion object {
        private const val MAX_LENGTH = 50

        fun of(tags: List<String>): EmotionTags {
            tags.forEach { tag ->
                if (tag.isBlank()) {
                    throw InvalidValueException(EmotionTags::class, tag, "감정 태그는 비어 있을 수 없습니다.")
                }
                if (tag.length > MAX_LENGTH) {
                    throw InvalidValueException(EmotionTags::class, tag, "감정 태그는 ${MAX_LENGTH}자 이하여야 합니다.")
                }
            }
            return EmotionTags(tags)
        }
    }
}
