package com.konkuk.ma.domain.xroom.repository

import com.konkuk.ma.domain.xroom.dao.MemoryQueryDao
import com.konkuk.ma.domain.xroom.domain.memory.Memory
import com.konkuk.ma.domain.xroom.domain.port.MemoryQueryRepository
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Repository

@Repository
class MemoryQueryCoreRepository(
    private val memoryQueryDao: MemoryQueryDao,
) : MemoryQueryRepository {
    override fun find(xroomId: Long): List<Memory> {
        return memoryQueryDao.find(xroomId).map { it.toDomain() }
    }

    override fun findOne(memoryId: Long): Memory {
        return memoryQueryDao.findOne(memoryId)?.toDomain()
            ?: throw EntityNotFoundException(EntityType.MEMORY, memoryId.toString())
    }

    override fun count(xroomIds: Set<Long>): Map<Long, Int> {
        return memoryQueryDao.count(xroomIds)
    }
}
