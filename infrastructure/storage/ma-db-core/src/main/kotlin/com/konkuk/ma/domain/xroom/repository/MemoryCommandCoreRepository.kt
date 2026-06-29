package com.konkuk.ma.domain.xroom.repository

import com.konkuk.ma.domain.xroom.dao.MemoryCommandDao
import com.konkuk.ma.domain.xroom.domain.memory.Memory
import com.konkuk.ma.domain.xroom.domain.memory.NewMemory
import com.konkuk.ma.domain.xroom.domain.port.MemoryCommandRepository
import org.springframework.stereotype.Repository

@Repository
class MemoryCommandCoreRepository(
    private val memoryCommandDao: MemoryCommandDao,
) : MemoryCommandRepository {
    override fun save(newMemory: NewMemory): Long {
        return memoryCommandDao.save(newMemory)
    }

    override fun update(memory: Memory, memberId: Long) {
        memoryCommandDao.update(memory, memberId)
    }

    override fun delete(memoryId: Long, memberId: Long) {
        memoryCommandDao.delete(memoryId, memberId)
    }
}
