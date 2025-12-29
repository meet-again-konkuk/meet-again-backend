package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.common.domain.Day
import com.konkuk.ma.domain.common.domain.Month
import com.konkuk.ma.domain.common.domain.Year
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.Region

class TargetInfo(
    val targetInfoId: Long,
    val registerEmail: String,
    val targetName: String,
    val targetGender: Gender,

    val middleNumber: String?,
    val lastNumber: String?,

    val year: Year?,
    val month: Month?,
    val day: Day?,

    val region: Region?
)
