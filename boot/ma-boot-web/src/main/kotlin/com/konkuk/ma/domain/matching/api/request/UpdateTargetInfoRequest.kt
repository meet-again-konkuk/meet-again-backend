package com.konkuk.ma.domain.matching.api.request

import com.konkuk.ma.support.validation.ValidationMessages
import com.konkuk.ma.support.validation.ValidationPatterns
import com.konkuk.ma.domain.member.domain.Region
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

class UpdateTargetInfoRequest(
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

    val region: Region?,
)
