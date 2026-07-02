package com.konkuk.ma.domain.xroom.domain.port

import com.konkuk.ma.domain.xroom.domain.memory.Memory
import com.konkuk.ma.domain.xroom.domain.memory.NewMemory

interface MemoryCommandRepository {
    fun save(newMemory: NewMemory): Long

    fun update(memory: Memory, memberId: Long)

    fun delete(memoryId: Long, memberId: Long)

    fun deleteByXrooms(xroomIds: Set<Long>, memberId: Long)
}
