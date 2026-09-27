package com.konkuk.ma.domain.matching.application

import com.konkuk.ma.domain.matching.domain.NewTargetInfo
import com.konkuk.ma.domain.matching.domain.TargetInfo
import com.konkuk.ma.domain.matching.domain.UpdateTargetInfo
import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
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
    private val matchingResultRepository: MatchingResultRepository,
    private val memberQueryRepository: MemberQueryRepository,
) {
    fun register(newTargetInfo: NewTargetInfo): Long {
        val member = memberQueryRepository.findOne(newTargetInfo.registerId)
        return targetInfoCommandRepository.save(newTargetInfo, member.getOtherGender())
    }

    fun update(id: Long, memberId: Long, updateTargetInfo: UpdateTargetInfo): TargetInfo {
        val targetInfo = targetInfoQueryRepository.findOne(id)
        targetInfo.validateOwnership(memberId)
        val hasMatchingResult = matchingResultRepository.exists(id)
        targetInfo.validateUpdatable(hasMatchingResult)
        targetInfoCommandRepository.update(id, memberId, updateTargetInfo)
        return targetInfoQueryRepository.findOne(id)
    }

    fun delete(id: Long, memberId: Long) {
        val targetInfo = targetInfoQueryRepository.findOne(id)
        targetInfo.validateOwnership(memberId)
        matchingResultRepository.delete(id, memberId)
        targetInfoCommandRepository.delete(id, memberId)
    }
}
