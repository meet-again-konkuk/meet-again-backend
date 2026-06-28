package com.konkuk.ma.domain.xroom.domain.memory

import com.konkuk.ma.exception.InvalidValueException

class MemoryTitle(val value: String) {

    init {
        if (value.isBlank()) {
            throw InvalidValueException(MemoryTitle::class, value, "기억 제목은 비어 있을 수 없습니다.")
        }
        if (value.length > MAX_LENGTH) {
            throw InvalidValueException(MemoryTitle::class, value, "기억 제목은 ${MAX_LENGTH}자 이하여야 합니다.")
        }
    }

    companion object {
        private const val MAX_LENGTH = 200
    }
}
