package com.konkuk.ma.domain.xroom.application

import com.konkuk.ma.domain.xroom.domain.NewXroom
import com.konkuk.ma.domain.xroom.domain.XroomValidator
import com.konkuk.ma.domain.xroom.domain.CreatedXroom
import com.konkuk.ma.domain.xroom.domain.memory.NewMemories
import com.konkuk.ma.domain.xroom.domain.port.MemoryCommandRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomCommandRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class XroomCommandService(
    private val xroomCommandRepository: XroomCommandRepository,
    private val xroomQueryRepository: XroomQueryRepository,
    private val memoryCommandRepository: MemoryCommandRepository,
    private val xroomValidator: XroomValidator,
) {
    fun create(targetInfoId: Long, memberId: Long, finalMessage: String?): Long {
        val newXroom = NewXroom(ownerId = memberId, targetInfoId = targetInfoId, finalMessage = finalMessage)
        xroomValidator.validate(newXroom)
        return xroomCommandRepository.save(newXroom)
    }

    fun createWithMemories(
        memberId: Long,
        targetInfoId: Long,
        finalMessage: String?,
        newMemories: NewMemories,
    ): CreatedXroom {
        val newXroom = NewXroom(ownerId = memberId, targetInfoId = targetInfoId, finalMessage = finalMessage)
        xroomValidator.validateAfterRemovingEmptyRoom(newXroom)
        val xroomId = xroomCommandRepository.save(newXroom)
        return CreatedXroom(xroomId, memoryCommandRepository.saveAll(newMemories.bindTo(xroomId)))
    }

    fun updateFinalMessage(xroomId: Long, memberId: Long, finalMessage: String?): Long {
        val xroom = xroomQueryRepository.findOne(xroomId)
        xroom.validateOwnership(memberId)
        val updated = xroom.updateFinalMessage(finalMessage)
        xroomCommandRepository.updateFinalMessage(updated)
        return updated.id
    }
}
