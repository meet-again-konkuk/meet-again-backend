package com.konkuk.ma.domain.auth.api.request

import jakarta.validation.constraints.Pattern

class SmsSendRequest(
    @field:Pattern(regexp = "^010\\d{8}$", message = "유효하지 않은 휴대폰 번호 형식입니다.")
    val phoneNumber: String
)
