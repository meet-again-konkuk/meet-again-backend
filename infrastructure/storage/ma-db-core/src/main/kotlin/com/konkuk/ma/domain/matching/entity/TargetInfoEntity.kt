package com.konkuk.ma.domain.matching.entity

import com.konkuk.ma.domain.common.domain.Day
import com.konkuk.ma.domain.common.domain.Month
import com.konkuk.ma.domain.common.domain.Year
import com.konkuk.ma.domain.member.domain.FourDigit
import com.konkuk.ma.domain.matching.domain.TargetInfo
import com.konkuk.ma.domain.member.domain.Gender
import com.konkuk.ma.domain.member.domain.Region
import java.time.LocalDateTime

class TargetInfoEntity(
    val id: Long,
    val registerEmail: String,
    val name: String,
    val targetGender: Gender,
    val middleNumber: String?,
    val lastNumber: String?,
    val year: Int?,
    val month: Int?,
    val day: Int?,
    val region: Region?,
    val createdDate: LocalDateTime,
    val lastModifiedDate: LocalDateTime
) {
    fun toDomain(): TargetInfo {
        return TargetInfo(
            targetInfoId = id,
            registerEmail = registerEmail,
            targetName = name,
            targetGender = targetGender,
            middleNumber = middleNumber?.let { FourDigit(it) },
            lastNumber = lastNumber?.let { FourDigit(it) },
            year = year?.let { Year(it) },
            month = month?.let { Month(it) },
            day = day?.let { Day(it) },
            region = region
        )
    }
}
