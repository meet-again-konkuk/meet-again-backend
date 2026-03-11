package com.konkuk.ma.domain.matching.fixture

import com.konkuk.ma.domain.common.domain.Day
import com.konkuk.ma.domain.common.domain.Month
import com.konkuk.ma.domain.common.domain.Year
import com.konkuk.ma.domain.matching.domain.Target
import com.konkuk.ma.domain.member.domain.FourDigit
import com.konkuk.ma.domain.member.domain.Region

object TargetFixture {
    fun create(
        email: String = "target@example.com",
        middleNumber: FourDigit = FourDigit("1234"),
        lastNumber: FourDigit = FourDigit("5678"),
        year: Year = Year(1999),
        month: Month = Month(12),
        day: Day = Day(31),
        region: Region = Region.SEOUL
    ): Target {
        return Target(
            email = email,
            middleNumber = middleNumber,
            lastNumber = lastNumber,
            year = year,
            month = month,
            day = day,
            region = region
        )
    }
}
