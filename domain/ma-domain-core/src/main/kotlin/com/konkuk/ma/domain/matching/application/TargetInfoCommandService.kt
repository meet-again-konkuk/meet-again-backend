package com.konkuk.ma.domain.matching.application

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.common.domain.date.Day
import com.konkuk.ma.domain.common.domain.date.Month
import com.konkuk.ma.domain.common.domain.date.Year
import com.konkuk.ma.domain.matching.domain.NewTargetInfo
import com.konkuk.ma.domain.matching.domain.TargetInfo
import com.konkuk.ma.domain.matching.domain.port.TargetInfoCommandRepository
import com.konkuk.ma.domain.matching.domain.port.TargetInfoQueryRepository
import com.konkuk.ma.domain.member.domain.FourDigit
import com.konkuk.ma.domain.member.domain.Region
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

    fun update(
        id: Long,
        email: String,
        targetName: String,
        middleNumber: FourDigit?,
        lastNumber: FourDigit?,
        year: Year?,
        month: Month?,
        day: Day?,
        region: Region?,
    ): TargetInfo {
        val targetInfo = targetInfoQueryRepository.findOne(id)
        targetInfo.validateOwnership(Email(email))
        val updated = targetInfo.update(targetName, middleNumber, lastNumber, year, month, day, region)
        targetInfoCommandRepository.update(updated)
        return updated
    }
}
