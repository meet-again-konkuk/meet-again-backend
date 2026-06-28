package com.konkuk.ma.domain.xroom.application

import com.konkuk.ma.domain.xroom.application.command.AddMemoryCommand
import com.konkuk.ma.domain.xroom.application.command.UpdateMemoryCommand
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
    fun addMemory(xroomId: Long, memberId: Long, command: AddMemoryCommand): Long {
        val xroom = xroomQueryRepository.findOne(xroomId)
        xroom.validateOwnership(memberId)
        val newMemory = NewMemory(
            xroomId = xroomId,
            title = command.title,
            eventDate = command.eventDate,
            eventDatePrecision = command.eventDatePrecision,
            location = command.location,
            emotionTags = command.emotionTags,
            text = command.text,
            letter = command.letter,
        )
        return memoryCommandRepository.save(newMemory)
    }

    fun updateMemory(xroomId: Long, memoryId: Long, memberId: Long, command: UpdateMemoryCommand): Long {
        val xroom = xroomQueryRepository.findOne(xroomId)
        xroom.validateOwnership(memberId)
        val memory = memoryQueryRepository.findOne(memoryId)
        memory.validateBelongsTo(xroomId)
        val newMemory = NewMemory(
            xroomId = xroomId,
            title = command.title,
            eventDate = command.eventDate,
            eventDatePrecision = command.eventDatePrecision,
            location = command.location,
            emotionTags = command.emotionTags,
            text = command.text,
            letter = command.letter,
        )
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
