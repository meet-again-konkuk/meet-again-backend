package com.konkuk.ma.domain.xroom.application

import com.konkuk.ma.domain.xroom.domain.memory.NewMemory
import com.konkuk.ma.domain.xroom.domain.port.MemoryCommandRepository
import com.konkuk.ma.domain.xroom.domain.port.MemoryQueryRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MemoryCommandService(
    private val xroomQueryRepository: XroomQueryRepository,
    private val memoryQueryRepository: MemoryQueryRepository,
    private val memoryCommandRepository: MemoryCommandRepository,
) {
    fun addMemory(memberId: Long, newMemory: NewMemory): Long {
        val xroom = xroomQueryRepository.findOne(newMemory.xroomId)
        xroom.validateOwnership(memberId)
        return memoryCommandRepository.save(newMemory)
    }

    fun updateMemory(memoryId: Long, memberId: Long, newMemory: NewMemory): Long {
        val xroom = xroomQueryRepository.findOne(newMemory.xroomId)
        xroom.validateOwnership(memberId)
        val memory = memoryQueryRepository.findOne(memoryId)
        memory.validateBelongsTo(newMemory.xroomId)
        val updated = memory.update(newMemory)
        memoryCommandRepository.update(updated, memberId)
        return updated.id
    }

    fun removeMemory(xroomId: Long, memoryId: Long, memberId: Long): Long {
        val xroom = xroomQueryRepository.findOne(xroomId)
        xroom.validateOwnership(memberId)
        val memory = memoryQueryRepository.findOne(memoryId)
        memory.validateBelongsTo(xroomId)
        memoryCommandRepository.delete(memoryId, memberId)
        return memoryId
    }
}
