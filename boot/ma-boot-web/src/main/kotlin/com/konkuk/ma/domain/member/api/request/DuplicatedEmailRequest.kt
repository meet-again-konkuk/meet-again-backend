package com.konkuk.ma.domain.member.api.request

import com.konkuk.ma.support.validation.ValidationMessages
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

class DuplicatedEmailRequest(
    @field:NotBlank(message = ValidationMessages.EMAIL_REQUIRED)
    @field:Email(message = ValidationMessages.EMAIL_INVALID)
    val email: String
) 