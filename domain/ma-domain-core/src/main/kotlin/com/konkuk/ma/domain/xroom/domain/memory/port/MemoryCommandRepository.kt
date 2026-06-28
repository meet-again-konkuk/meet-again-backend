package com.konkuk.ma.domain.xroom.domain.memory.port

import com.konkuk.ma.domain.xroom.domain.memory.NewMemory

interface MemoryCommandRepository {
    fun save(newMemory: NewMemory): Long
}
