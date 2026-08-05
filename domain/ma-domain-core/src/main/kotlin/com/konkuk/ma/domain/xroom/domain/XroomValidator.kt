package com.konkuk.ma.domain.xroom.domain

import com.konkuk.ma.domain.matching.domain.MatchingResults
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.domain.matching.domain.port.TargetInfoQueryRepository
import com.konkuk.ma.domain.xroom.domain.memory.Memory
import com.konkuk.ma.domain.xroom.domain.memory.MemoryCounts
import com.konkuk.ma.domain.xroom.domain.port.MemoryQueryRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomCommandRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomQueryRepository
import com.konkuk.ma.exception.DuplicateException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Component

@Component
class XroomValidator(
    private val targetInfoQueryRepository: TargetInfoQueryRepository,
    private val xroomQueryRepository: XroomQueryRepository,
    private val xroomCommandRepository: XroomCommandRepository,
    private val memoryQueryRepository: MemoryQueryRepository,
    private val matchingResultRepository: MatchingResultRepository,
) {
    fun validate(newXroom: NewXroom) {
        validateOwnership(newXroom)
        validateNotDuplicated(newXroom.targetInfoId)
    }

    fun validateAfterRemovingEmptyRoom(newXroom: NewXroom) {
        validateOwnership(newXroom)
        val existing = xroomQueryRepository.findByTargetInfoIdOrNull(newXroom.targetInfoId) ?: return
        removeEmptyRoom(existing, newXroom.ownerId)
    }

    fun validateAccessible(xroom: Xroom, memberId: Long) {
        if (xroom.isOwnedBy(memberId)) return

        val receivedTargetInfoIds =
            MatchingResults(matchingResultRepository.findClaimedByTarget(memberId)).extractTargetInfoIds()
        xroom.validateRecipient(receivedTargetInfoIds)
    }

    fun validateOwnedXroom(xroomId: Long, memberId: Long) {
        val xroom = xroomQueryRepository.findOne(xroomId)
        xroom.validateOwnership(memberId)
    }

    fun validateOwnedMemory(xroomId: Long, memoryId: Long, memberId: Long): Memory {
        validateOwnedXroom(xroomId, memberId)
        val memory = memoryQueryRepository.findOne(memoryId)
        memory.validateBelongsTo(xroomId)
        return memory
    }

    private fun validateOwnership(newXroom: NewXroom) {
        val targetInfo = targetInfoQueryRepository.findOne(newXroom.targetInfoId)
        targetInfo.validateOwnership(newXroom.ownerId)
    }

    private fun removeEmptyRoom(xroom: Xroom, memberId: Long) {
        validateHasNoMemory(xroom)
        xroomCommandRepository.deleteById(xroom.id, memberId)
    }

    private fun validateHasNoMemory(xroom: Xroom) {
        val memoryCounts = MemoryCounts(memoryQueryRepository.count(setOf(xroom.id)))
        if (!memoryCounts.hasNoMemory(xroom.id)) {
            throw DuplicateException(EntityType.XROOM, "targetInfoId", xroom.targetInfoId.toString())
        }
    }

    private fun validateNotDuplicated(targetInfoId: Long) {
        if (xroomQueryRepository.exists(targetInfoId)) {
            throw DuplicateException(EntityType.XROOM, "targetInfoId", targetInfoId.toString())
        }
    }
}
