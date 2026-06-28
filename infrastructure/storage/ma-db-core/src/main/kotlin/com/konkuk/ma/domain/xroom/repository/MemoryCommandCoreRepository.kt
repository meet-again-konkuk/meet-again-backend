package com.konkuk.ma.domain.xroom.repository

import com.konkuk.ma.domain.xroom.dao.MemoryCommandDao
import com.konkuk.ma.domain.xroom.domain.memory.NewMemory
import com.konkuk.ma.domain.xroom.domain.memory.port.MemoryCommandRepository
import org.springframework.stereotype.Repository

@Repository
class MemoryCommandCoreRepository(
    private val memoryCommandDao: MemoryCommandDao,
) : MemoryCommandRepository {
    override fun save(newMemory: NewMemory): Long {
        return memoryCommandDao.save(newMemory)
    }
}
