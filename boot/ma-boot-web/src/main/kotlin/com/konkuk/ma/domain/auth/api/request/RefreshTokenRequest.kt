package com.konkuk.ma.domain.auth.api.request

import com.konkuk.ma.support.validation.ValidationMessages
import jakarta.validation.constraints.NotBlank

data class RefreshTokenRequest(
    @field:NotBlank(message = ValidationMessages.REFRESH_TOKEN_REQUIRED)
    val refreshToken: String
)
