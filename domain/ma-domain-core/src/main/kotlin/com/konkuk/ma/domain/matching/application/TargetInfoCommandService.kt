package com.konkuk.ma.domain.matching.application

import com.konkuk.ma.domain.matching.domain.NewTargetInfo
import com.konkuk.ma.domain.matching.domain.port.TargetInfoCommandRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class TargetInfoCommandService(
    private val targetInfoCommandRepository: TargetInfoCommandRepository,
    private val memberQueryRepository: MemberQueryRepository
) {
    fun register(newTargetInfo: NewTargetInfo): Long {
        val member = memberQueryRepository.findOne(newTargetInfo.registerEmail)
        return targetInfoCommandRepository.save(newTargetInfo, member.getOtherGender())
    }
}
