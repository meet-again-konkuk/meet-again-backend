package com.konkuk.ma.domain.auth.api.request

import com.konkuk.ma.domain.auth.application.command.LoginCommand
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:Email
    @field:NotBlank
    val email: String,

    @field:NotBlank
    val password: String
) {
    fun toCommand() = LoginCommand(email, password)
}
