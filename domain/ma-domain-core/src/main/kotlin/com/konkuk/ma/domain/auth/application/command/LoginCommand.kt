package com.konkuk.ma.domain.auth.application.command

import com.konkuk.ma.domain.common.domain.Email

class LoginCommand(
    val email: Email,

    val password: String
)
