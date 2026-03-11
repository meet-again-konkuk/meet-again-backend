package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.common.domain.Day
import com.konkuk.ma.domain.common.domain.Month
import com.konkuk.ma.domain.common.domain.Year
import com.konkuk.ma.domain.member.domain.FourDigit
import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.Region

class Target(
    val email: String,
    val middleNumber: FourDigit,
    val lastNumber: FourDigit,

    val year: Year,
    val month: Month,
    val day: Day,

    val region: Region
) {
    companion object {
        fun create(member: Member): Target {
            return Target(
                email = member.email,
                middleNumber = member.phoneNumber.middleNumber,
                lastNumber = member.phoneNumber.lastNumber,
                year = member.getYear(),
                month = member.getMonth(),
                day = member.getDay(),
                region = member.region
            )
        }
    }
}
