package com.konkuk.ma.domain.xroom.domain

import com.konkuk.ma.domain.matching.domain.port.TargetInfoQueryRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomQueryRepository
import com.konkuk.ma.exception.DuplicateException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Component

@Component
class XroomValidator(
    private val targetInfoQueryRepository: TargetInfoQueryRepository,
    private val xroomQueryRepository: XroomQueryRepository,
) {
    fun validate(newXroom: NewXroom) {
        validateOwnership(newXroom)
        validateNotDuplicated(newXroom.targetInfoId)
    }

    private fun validateOwnership(newXroom: NewXroom) {
        val targetInfo = targetInfoQueryRepository.findOne(newXroom.targetInfoId)
        targetInfo.validateOwnership(newXroom.ownerId)
    }

    private fun validateNotDuplicated(targetInfoId: Long) {
        if (xroomQueryRepository.exists(targetInfoId)) {
            throw DuplicateException(EntityType.XROOM, "targetInfoId", targetInfoId.toString())
        }
    }
}
