package com.konkuk.ma.domain.member.api.request

import com.konkuk.ma.support.validation.ValidationMessages
import com.konkuk.ma.support.validation.ValidationPatterns
import jakarta.validation.constraints.Pattern

class DuplicatedNicknameRequest(
    @field:Pattern(regexp = ValidationPatterns.NICKNAME, message = ValidationMessages.NICKNAME_INVALID)
    val nickname: String
)
