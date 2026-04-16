package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.common.domain.date.ONE_DAY
import com.konkuk.ma.domain.common.domain.date.hasElapsed
import com.konkuk.ma.domain.common.domain.date.Day
import com.konkuk.ma.domain.common.domain.date.Month
import com.konkuk.ma.domain.common.domain.date.Year
import com.konkuk.ma.domain.member.domain.FourDigit
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.Region
import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.EntityType
import com.konkuk.ma.exception.InvalidStateException
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class TargetInfo(
    val targetInfoId: Long,
    val registerEmail: Email,
    val targetName: String,
    val targetGender: Gender,

    val middleNumber: FourDigit?,
    val lastNumber: FourDigit?,
    val year: Year?,
    val month: Month?,
    val day: Day?,

    val region: Region?,

    val createdDate: LocalDateTime = LocalDateTime.now(),
) {
    fun validateUpdatable(hasMatchingResult: Boolean) {
        if (hasMatchingResult) {
            throw InvalidStateException(TargetInfo::class, targetInfoId, "매칭 결과가 존재하여 수정할 수 없습니다.")
        }
        if (createdDate.hasElapsed(ONE_DAY, ChronoUnit.DAYS)) {
            throw InvalidStateException(TargetInfo::class, targetInfoId, "생성 후 하루가 경과하여 수정할 수 없습니다.")
        }
    }

    fun validateOwnership(email: Email) {
        if (registerEmail != email) {
            throw AccessDeniedException(EntityType.TARGET_INFO, registerEmail.value, email.value)
        }
    }

    fun makeMatchingResults(targets: Targets): NewMatchingResults {
        val results = targets
            .filterCandidates(targetName, targetGender)
            .map { makeMatchingResult(it) }
        return NewMatchingResults(results)
    }


    private fun makeMatchingResult(target: Target): NewMatchingResult {
        val middleNumberMatched = middleNumber == target.middleNumber
        val lastNumberMatched = lastNumber == target.lastNumber

        val yearMatched = year == target.year
        val monthMatched = month == target.month
        val dayMatched = day == target.day

        val regionMatched = region == target.region

        return NewMatchingResult(
            registerEmail = registerEmail,
            targetInfoId = targetInfoId,
            targetEmail = target.email,
            middleNumberMatched = middleNumberMatched,
            lastNumberMatched = lastNumberMatched,
            yearMatched = yearMatched,
            monthMatched = monthMatched,
            dayMatched = dayMatched,
            regionMatched = regionMatched,
        )
    }
}
