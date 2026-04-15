package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.common.domain.date.Day
import com.konkuk.ma.domain.common.domain.date.Month
import com.konkuk.ma.domain.common.domain.date.Year
import com.konkuk.ma.domain.member.domain.FourDigit
import com.konkuk.ma.domain.member.domain.Region

class UpdateTargetInfo(
    middleNumber: String?,
    lastNumber: String?,
    year: Int?,
    month: Int?,
    day: Int?,
    val region: Region?,
) {
    val middleNumber: FourDigit? = middleNumber?.let { FourDigit(it) }
    val lastNumber: FourDigit? = lastNumber?.let { FourDigit(it) }
    val year: Year? = year?.let { Year(it) }
    val month: Month? = month?.let { Month(it) }
    val day: Day? = day?.let { Day(it) }
}
