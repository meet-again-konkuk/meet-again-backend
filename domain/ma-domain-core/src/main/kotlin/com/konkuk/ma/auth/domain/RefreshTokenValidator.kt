package com.konkuk.ma.auth.domain

import com.konkuk.ma.auth.domain.port.RefreshTokenRepository
import org.springframework.stereotype.Component

@Component
class RefreshTokenValidator(
    private val refreshTokenRepository: RefreshTokenRepository
) {
    fun validate(refreshToken: RefreshToken) {
        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken.email)
            throw RuntimeException("Refresh token expired. email: ${refreshToken.email}, expired date: ${refreshToken.expirationDate}")
        }
    }
}
