package com.konkuk.ma.domain.xroom.application

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.matching.domain.port.TargetInfoQueryRepository
import com.konkuk.ma.domain.xroom.domain.NewXroom
import com.konkuk.ma.domain.xroom.domain.port.XroomCommandRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomQueryRepository
import com.konkuk.ma.exception.DuplicateException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class XroomCommandService(
    private val targetInfoQueryRepository: TargetInfoQueryRepository,
    private val xroomCommandRepository: XroomCommandRepository,
    private val xroomQueryRepository: XroomQueryRepository,
) {
    fun create(targetInfoId: Long, email: String): Long {
        val targetInfo = targetInfoQueryRepository.findOne(targetInfoId)
        targetInfo.validateOwnership(Email(email))

        if (xroomQueryRepository.existsByTargetInfoId(targetInfoId)) {
            throw DuplicateException(EntityType.XROOM, "targetInfoId", targetInfoId.toString())
        }

        val newXroom = NewXroom(ownerEmail = email, targetInfoId = targetInfoId)
        return xroomCommandRepository.save(newXroom)
    }
}
