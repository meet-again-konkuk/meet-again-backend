package com.konkuk.ma.domain.xroom.application

import com.konkuk.ma.domain.xroom.domain.XroomValidator
import com.konkuk.ma.domain.xroom.domain.memory.MemoryDetails
import com.konkuk.ma.domain.xroom.domain.memory.NewMemory
import com.konkuk.ma.domain.xroom.domain.port.MediaCommandRepository
import com.konkuk.ma.domain.xroom.domain.port.MemoryCommandRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MemoryCommandService(
    private val memoryCommandRepository: MemoryCommandRepository,
    private val mediaCommandRepository: MediaCommandRepository,
    private val xroomValidator: XroomValidator,
) {
    fun addMemory(memberId: Long, newMemory: NewMemory): Long {
        xroomValidator.validateOwnedXroom(newMemory.xroomId, memberId)
        return memoryCommandRepository.save(newMemory)
    }

    fun updateMemory(xroomId: Long, memoryId: Long, memberId: Long, details: MemoryDetails): Long {
        val memory = xroomValidator.validateOwnedMemory(xroomId, memoryId, memberId)
        val updated = memory.update(details)
        memoryCommandRepository.update(updated, memberId)
        return updated.id
    }

    fun removeMemory(xroomId: Long, memoryId: Long, memberId: Long): Long {
        xroomValidator.validateOwnedMemory(xroomId, memoryId, memberId)
        memoryCommandRepository.delete(memoryId, memberId)
        mediaCommandRepository.softDeleteByMemory(memoryId, memberId)
        return memoryId
    }
}
