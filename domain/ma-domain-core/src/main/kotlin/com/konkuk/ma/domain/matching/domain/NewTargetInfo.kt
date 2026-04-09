package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.common.domain.date.Day
import com.konkuk.ma.domain.common.domain.date.Month
import com.konkuk.ma.domain.common.domain.date.Year
import com.konkuk.ma.domain.member.domain.FourDigit
import com.konkuk.ma.domain.member.domain.Region

class NewTargetInfo(
    val registerEmail: Email,
    val targetName: String,
    val middleNumber: FourDigit?,
    val lastNumber: FourDigit?,

    val year: Year?,
    val month: Month?,
    val day: Day?,

    val region: Region?
)
