package com.konkuk.ma.domain.auth.application.command

import com.konkuk.ma.domain.common.domain.Email

class WithdrawalCancelCommand(
    email: String,
    val password: String
) {
    val email: Email = Email(email)
}
