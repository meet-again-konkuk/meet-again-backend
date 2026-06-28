package com.konkuk.ma.domain.xroom.application

import com.konkuk.ma.domain.xroom.application.command.AddMemoryCommand
import com.konkuk.ma.domain.xroom.domain.memory.NewMemory
import com.konkuk.ma.domain.xroom.domain.memory.port.MemoryCommandRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MemoryCommandService(
    private val xroomQueryRepository: XroomQueryRepository,
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
}
