package com.konkuk.ma.domain.matching.api.request

import com.konkuk.ma.domain.matching.domain.UpdateTargetInfo
import com.konkuk.ma.domain.member.domain.Region
import com.konkuk.ma.support.validation.ValidationMessages
import com.konkuk.ma.support.validation.ValidationPatterns
import jakarta.validation.constraints.Pattern

class UpdateTargetInfoRequest(
    @field:Pattern(regexp = ValidationPatterns.FOUR_DIGIT, message = ValidationMessages.FOUR_DIGIT_MIDDLE_INVALID)
    val middleNumber: String?,

    @field:Pattern(regexp = ValidationPatterns.FOUR_DIGIT, message = ValidationMessages.FOUR_DIGIT_LAST_INVALID)
    val lastNumber: String?,

    val year: Int?,
    val month: Int?,
    val day: Int?,

    val region: Region?,
) {
    fun toUpdateTargetInfo(): UpdateTargetInfo {
        return UpdateTargetInfo(
            middleNumber = middleNumber,
            lastNumber = lastNumber,
            year = year,
            month = month,
            day = day,
            region = region,
        )
    }
}
