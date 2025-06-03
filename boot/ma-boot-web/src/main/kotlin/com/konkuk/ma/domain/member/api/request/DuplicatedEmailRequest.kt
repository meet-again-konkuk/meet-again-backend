package com.konkuk.ma.domain.member.api.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

class DuplicatedEmailRequest(
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "유효하지 않은 이메일 형식입니다.")
    val email: String
) 