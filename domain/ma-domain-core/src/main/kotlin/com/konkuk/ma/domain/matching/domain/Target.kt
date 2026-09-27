package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.common.domain.date.Day
import com.konkuk.ma.domain.common.domain.date.Month
import com.konkuk.ma.domain.common.domain.date.Year
import com.konkuk.ma.domain.member.domain.FourDigit
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.Member
import com.konkuk.ma.domain.member.domain.Region

class Target(
    val memberId: Long,
    val name: String,
    val gender: Gender,
    val middleNumber: FourDigit,
    val lastNumber: FourDigit,

    val year: Year,
    val month: Month,
    val day: Day,

    val region: Region
) {
    fun matchesNameAndGender(name: String, gender: Gender): Boolean {
        return this.name == name && this.gender == gender
    }

    companion object {
        fun create(member: Member): Target {
            return Target(
                memberId = member.id,
                name = member.name,
                gender = member.gender,
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
