package com.konkuk.ma.domain.xroom.domain.memory

import com.konkuk.ma.exception.InvalidValueException

class NewMemories(
    val data: List<MemoryDetails>,
) {
    init {
        if (data.isEmpty()) {
            throw InvalidValueException(NewMemories::class, data.size, "기억은 최소 1개 이상이어야 합니다.")
        }
        if (data.size > MAX_SIZE) {
            throw InvalidValueException(NewMemories::class, data.size, "기억은 최대 ${MAX_SIZE}개까지 등록할 수 있습니다.")
        }
    }

    fun bindTo(xroomId: Long): List<NewMemory> {
        return data.map { NewMemory(xroomId = xroomId, details = it) }
    }

    companion object {
        private const val MAX_SIZE = 50
    }
}
