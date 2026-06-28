package com.konkuk.ma.domain.xroom.domain.port

import com.konkuk.ma.domain.xroom.domain.memory.Memory

interface MemoryQueryRepository {
    fun find(xroomId: Long): List<Memory>

    fun findOne(memoryId: Long): Memory

    fun count(xroomIds: Set<Long>): Map<Long, Int>
}
