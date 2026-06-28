package com.konkuk.ma.domain.xroom.domain.memory.port

import com.konkuk.ma.domain.xroom.domain.memory.Memory

interface MemoryQueryRepository {
    fun find(xroomId: Long): List<Memory>

    fun count(xroomIds: Set<Long>): Map<Long, Int>
}
