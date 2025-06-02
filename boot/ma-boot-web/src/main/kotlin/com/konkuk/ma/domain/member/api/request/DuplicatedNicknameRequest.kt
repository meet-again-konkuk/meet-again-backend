package com.konkuk.ma.domain.member.api.request

import jakarta.validation.constraints.Pattern

class DuplicatedNicknameRequest(
    @field:Pattern(regexp = "^[a-zA-Z가-힣]{2,8}$", message = "유효하지 않은 닉네임 형식입니다.")
    val nickname: String
)
