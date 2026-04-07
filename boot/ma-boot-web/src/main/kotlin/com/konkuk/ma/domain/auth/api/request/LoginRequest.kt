package com.konkuk.ma.domain.auth.api.request

import com.konkuk.ma.domain.auth.application.command.LoginCommand
import com.konkuk.ma.support.validation.ValidationMessages
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = ValidationMessages.EMAIL_REQUIRED)
    @field:Email(message = ValidationMessages.EMAIL_INVALID)
    val email: String,

    @field:NotBlank(message = ValidationMessages.PASSWORD_REQUIRED)
    val password: String
) {
    fun toCommand() = LoginCommand(email, password)
}
