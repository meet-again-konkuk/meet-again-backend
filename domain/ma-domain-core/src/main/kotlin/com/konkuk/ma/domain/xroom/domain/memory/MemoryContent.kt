package com.konkuk.ma.domain.xroom.domain.memory

import com.konkuk.ma.exception.InvalidValueException

class MemoryContent private constructor(
    val text: String?,
    val letter: String?,
) {
    companion object {
        fun of(text: String?, letter: String?): MemoryContent {
            val normalizedText = text?.takeIf { it.isNotBlank() }
            val normalizedLetter = letter?.takeIf { it.isNotBlank() }
            if (normalizedText != null && normalizedLetter != null) {
                throw InvalidValueException(
                    MemoryContent::class,
                    "text, letter",
                    "텍스트와 편지는 함께 입력할 수 없습니다.",
                )
            }
            return MemoryContent(normalizedText, normalizedLetter)
        }
    }
}
