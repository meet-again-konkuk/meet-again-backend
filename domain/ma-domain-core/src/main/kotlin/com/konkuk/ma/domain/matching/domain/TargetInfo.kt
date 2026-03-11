package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.common.HasCursorId
import com.konkuk.ma.domain.common.domain.Day
import com.konkuk.ma.domain.common.domain.Month
import com.konkuk.ma.domain.common.domain.Year
import com.konkuk.ma.domain.member.domain.FourDigit
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.Region

class TargetInfo(
    val targetInfoId: Long,
    val registerEmail: String,
    val targetName: String,
    val targetGender: Gender,

    val middleNumber: FourDigit?,
    val lastNumber: FourDigit?,

    val year: Year?,
    val month: Month?,
    val day: Day?,

    val region: Region?
) : HasCursorId<Long> {
    override val cursorId: Long
        get() = targetInfoId

    fun makeMatchingResults(targets: Targets): MatchingResults {
        val results = targets
            .filterCandidates(targetName, targetGender)
            .map { makeMatchingResult(it) }
        return MatchingResults(results)
    }

    private fun makeMatchingResult(target: Target): MatchingResult {
        val middleNumberMatched = middleNumber == target.middleNumber
        val lastNumberMatched = lastNumber == target.lastNumber

        val yearMatched = year == target.year
        val monthMatched = month == target.month
        val dayMatched = day == target.day

        val regionMatched = region == target.region

        return MatchingResult(
            registerEmail,
            targetInfoId,
            target.email,
            middleNumberMatched,
            lastNumberMatched,
            yearMatched,
            monthMatched,
            dayMatched,
            regionMatched
        )
    }
}
