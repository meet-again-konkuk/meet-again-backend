package com.konkuk.ma.domain.auth.api.request

import com.konkuk.ma.support.validation.ValidationMessages
import com.konkuk.ma.support.validation.ValidationPatterns
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class EmailRecoveryRequest(
    @field:NotBlank(message = ValidationMessages.NAME_REQUIRED)
    @field:Pattern(regexp = ValidationPatterns.NAME, message = ValidationMessages.NAME_INVALID)
    val name: String,

    @field:NotBlank(message = ValidationMessages.PHONE_NUMBER_REQUIRED)
    @field:Pattern(regexp = ValidationPatterns.PHONE_NUMBER, message = ValidationMessages.PHONE_NUMBER_INVALID)
    val phone: String
)
