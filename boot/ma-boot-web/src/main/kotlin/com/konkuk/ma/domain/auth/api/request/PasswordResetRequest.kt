package com.konkuk.ma.domain.auth.api.request

import com.konkuk.ma.support.validation.ValidationMessages
import com.konkuk.ma.support.validation.ValidationPatterns
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class PasswordResetRequest(
    @field:NotBlank(message = ValidationMessages.EMAIL_REQUIRED)
    @field:Email(message = ValidationMessages.EMAIL_INVALID)
    val email: String,

    @field:NotBlank(message = ValidationMessages.NAME_REQUIRED)
    @field:Pattern(regexp = ValidationPatterns.NAME, message = ValidationMessages.NAME_INVALID)
    val name: String,

    @field:NotBlank(message = ValidationMessages.PHONE_NUMBER_REQUIRED)
    @field:Pattern(regexp = ValidationPatterns.PHONE_NUMBER, message = ValidationMessages.PHONE_NUMBER_INVALID)
    val phone: String,

    @field:NotBlank(message = ValidationMessages.PASSWORD_REQUIRED)
    @field:Pattern(regexp = ValidationPatterns.PASSWORD, message = ValidationMessages.PASSWORD_INVALID)
    val newPassword: String
)
