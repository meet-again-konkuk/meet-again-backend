package com.konkuk.ma.domain.xroom.domain

import com.konkuk.ma.domain.matching.domain.port.TargetInfoQueryRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomQueryRepository
import com.konkuk.ma.exception.DuplicateException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Component

@Component
class XroomValidator(
    private val targetInfoQueryRepository: TargetInfoQueryRepository,
    private val memberQueryRepository: MemberQueryRepository,
    private val xroomQueryRepository: XroomQueryRepository,
) {
    fun validate(newXroom: NewXroom) {
        validateOwnership(newXroom)
        validateNotDuplicated(newXroom.targetInfoId)
    }

    private fun validateOwnership(newXroom: NewXroom) {
        // Xroom 도메인은 아직 email 참조라, TargetInfo(memberId 참조) 소유권 검증을 위해 email→id로 브릿지한다. (Phase 2e에서 제거)
        val owner = memberQueryRepository.findOne(newXroom.ownerEmail)
        val targetInfo = targetInfoQueryRepository.findOne(newXroom.targetInfoId)
        targetInfo.validateOwnership(owner.id)
    }

    private fun validateNotDuplicated(targetInfoId: Long) {
        if (xroomQueryRepository.exists(targetInfoId)) {
            throw DuplicateException(EntityType.XROOM, "targetInfoId", targetInfoId.toString())
        }
    }
}
