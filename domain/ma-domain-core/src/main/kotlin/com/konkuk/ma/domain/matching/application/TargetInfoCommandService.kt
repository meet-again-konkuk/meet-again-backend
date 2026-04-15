package com.konkuk.ma.domain.matching.application

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.matching.domain.NewTargetInfo
import com.konkuk.ma.domain.matching.domain.TargetInfo
import com.konkuk.ma.domain.matching.domain.UpdateTargetInfo
import com.konkuk.ma.domain.matching.domain.port.TargetInfoCommandRepository
import com.konkuk.ma.domain.matching.domain.port.TargetInfoQueryRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class TargetInfoCommandService(
    private val targetInfoCommandRepository: TargetInfoCommandRepository,
    private val targetInfoQueryRepository: TargetInfoQueryRepository,
    private val memberQueryRepository: MemberQueryRepository,
) {
    fun register(newTargetInfo: NewTargetInfo): Long {
        val member = memberQueryRepository.findOne(newTargetInfo.registerEmail)
        return targetInfoCommandRepository.save(newTargetInfo, member.getOtherGender())
    }

    fun update(id: Long, email: String, updateTargetInfo: UpdateTargetInfo): TargetInfo {
        val targetInfo = targetInfoQueryRepository.findOne(id)
        targetInfo.validateOwnership(Email(email))
        val updated = targetInfo.update(
            targetName = updateTargetInfo.targetName,
            middleNumber = updateTargetInfo.middleNumber,
            lastNumber = updateTargetInfo.lastNumber,
            year = updateTargetInfo.year,
            month = updateTargetInfo.month,
            day = updateTargetInfo.day,
            region = updateTargetInfo.region,
        )
        targetInfoCommandRepository.update(updated)
        return updated
    }
}
