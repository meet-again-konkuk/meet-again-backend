package com.konkuk.ma.domain.matching.fixture

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.common.domain.date.Day
import com.konkuk.ma.domain.common.domain.date.Month
import com.konkuk.ma.domain.common.domain.date.Year
import com.konkuk.ma.domain.matching.domain.TargetInfo
import com.konkuk.ma.domain.member.domain.FourDigit
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.Region
import java.time.LocalDateTime

object TargetInfoFixture {
    fun create(
        targetInfoId: Long = 1L,
        registerEmail: String = "register@example.com",
        targetName: String = "홍길동",
        targetGender: Gender = Gender.MALE,
        middleNumber: FourDigit? = FourDigit("1234"),
        lastNumber: FourDigit? = FourDigit("5678"),
        year: Year? = Year(1999),
        month: Month? = Month(12),
        day: Day? = Day(31),
        region: Region? = Region.SEOUL,
        createdDate: LocalDateTime = LocalDateTime.now(),
    ): TargetInfo {
        return TargetInfo(
            targetInfoId = targetInfoId,
            registerEmail = Email(registerEmail),
            targetName = targetName,
            targetGender = targetGender,
            middleNumber = middleNumber,
            lastNumber = lastNumber,
            year = year,
            month = month,
            day = day,
            region = region,
            createdDate = createdDate,
        )
    }
}
