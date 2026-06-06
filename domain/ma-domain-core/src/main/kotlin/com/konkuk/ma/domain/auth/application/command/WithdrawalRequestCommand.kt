package com.konkuk.ma.domain.auth.application.command

import com.konkuk.ma.domain.common.domain.Email

class WithdrawalRequestCommand(
    email: String,
    val password: String
) {
    val email: Email = Email(email)
}
