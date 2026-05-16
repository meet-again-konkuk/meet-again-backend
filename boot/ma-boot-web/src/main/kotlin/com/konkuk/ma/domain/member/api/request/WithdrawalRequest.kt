package com.konkuk.ma.domain.member.api.request

import com.konkuk.ma.domain.member.application.command.WithdrawalRequestCommand
import com.konkuk.ma.support.validation.ValidationMessages
import jakarta.validation.constraints.NotBlank

data class WithdrawalRequest(
    @field:NotBlank(message = ValidationMessages.PASSWORD_REQUIRED)
    val password: String
) {
    fun toCommand(email: String) = WithdrawalRequestCommand(email, password)
}
