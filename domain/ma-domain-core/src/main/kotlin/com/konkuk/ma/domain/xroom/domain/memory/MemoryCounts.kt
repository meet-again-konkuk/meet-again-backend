package com.konkuk.ma.domain.xroom.domain.memory

class MemoryCounts(
    private val data: Map<Long, Int>,
) {
    fun countOf(xroomId: Long): Int = data[xroomId] ?: NO_MEMORY

    fun hasNoMemory(xroomId: Long): Boolean = countOf(xroomId) == NO_MEMORY

    companion object {
        private const val NO_MEMORY = 0
    }
}
