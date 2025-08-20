package com.konkuk.ma.domain.member.api.request

import com.konkuk.ma.auth.application.command.LoginCommand

class LoginRequest(
    val email: String,

    val password: String
) {
    fun toLoginCommand() = LoginCommand(email, password)
}
