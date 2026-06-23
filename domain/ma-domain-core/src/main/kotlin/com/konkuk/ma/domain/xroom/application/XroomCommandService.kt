package com.konkuk.ma.domain.xroom.application

import com.konkuk.ma.domain.xroom.domain.NewXroom
import com.konkuk.ma.domain.xroom.domain.XroomValidator
import com.konkuk.ma.domain.xroom.domain.port.XroomCommandRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class XroomCommandService(
    private val xroomCommandRepository: XroomCommandRepository,
    private val xroomValidator: XroomValidator,
) {
    fun create(targetInfoId: Long, memberId: Long): Long {
        val newXroom = NewXroom(ownerId = memberId, targetInfoId = targetInfoId)
        xroomValidator.validate(newXroom)
        return xroomCommandRepository.save(newXroom)
    }
}
