package com.konkuk.ma.domain.xroom.application

import com.konkuk.ma.domain.xroom.domain.NewXroom
import com.konkuk.ma.domain.xroom.domain.XroomValidator
import com.konkuk.ma.domain.xroom.domain.port.XroomCommandRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class XroomCommandService(
    private val xroomCommandRepository: XroomCommandRepository,
    private val xroomQueryRepository: XroomQueryRepository,
    private val xroomValidator: XroomValidator,
) {
    fun create(targetInfoId: Long, memberId: Long, finalMessage: String?): Long {
        val newXroom = NewXroom(ownerId = memberId, targetInfoId = targetInfoId, finalMessage = finalMessage)
        xroomValidator.validate(newXroom)
        return xroomCommandRepository.save(newXroom)
    }

    fun updateFinalMessage(xroomId: Long, memberId: Long, finalMessage: String?): Long {
        val xroom = xroomQueryRepository.findOne(xroomId)
        xroom.validateOwnership(memberId)
        val updated = xroom.updateFinalMessage(finalMessage)
        xroomCommandRepository.updateFinalMessage(updated)
        return updated.id
    }
}
