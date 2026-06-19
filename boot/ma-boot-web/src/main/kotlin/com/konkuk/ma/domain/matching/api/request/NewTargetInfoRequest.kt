package com.konkuk.ma.domain.matching.api.request

import com.konkuk.ma.support.validation.ValidationMessages
import com.konkuk.ma.support.validation.ValidationPatterns
import com.konkuk.ma.domain.common.domain.date.Day
import com.konkuk.ma.domain.common.domain.date.Month
import com.konkuk.ma.domain.common.domain.date.Year
import com.konkuk.ma.domain.matching.domain.NewTargetInfo
import com.konkuk.ma.domain.member.domain.FourDigit
import com.konkuk.ma.domain.member.domain.Region
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

class NewTargetInfoRequest(
    @field:NotBlank(message = ValidationMessages.NAME_REQUIRED)
    @field:Pattern(regexp = ValidationPatterns.NAME, message = ValidationMessages.NAME_INVALID)
    val name: String,

    @field:Pattern(regexp = ValidationPatterns.FOUR_DIGIT, message = ValidationMessages.FOUR_DIGIT_MIDDLE_INVALID)
    val middleNumber: String?,

    @field:Pattern(regexp = ValidationPatterns.FOUR_DIGIT, message = ValidationMessages.FOUR_DIGIT_LAST_INVALID)
    val lastNumber: String?,

    val year: Int?,
    val month: Int?,
    val day: Int?,

    val region: Region?
) {
    fun toNewTargetInfo(registerId: Long): NewTargetInfo {
        return NewTargetInfo(
            registerId = registerId,
            targetName = name,
            middleNumber = middleNumber?.let { FourDigit(it) },
            lastNumber = lastNumber?.let { FourDigit(it) },
            year = year?.let { Year(it) },
            month = month?.let { Month(it) },
            day = day?.let { Day(it) },
            region = region
        )
    }
}
