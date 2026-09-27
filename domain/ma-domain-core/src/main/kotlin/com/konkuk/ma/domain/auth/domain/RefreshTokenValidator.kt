package com.konkuk.ma.domain.auth.domain

import com.konkuk.ma.domain.auth.domain.port.RefreshTokenRepository
import com.konkuk.ma.domain.auth.exception.RefreshTokenExpiredException
import org.springframework.stereotype.Component

@Component
class RefreshTokenValidator(
    private val refreshTokenRepository: RefreshTokenRepository
) {
    fun validate(refreshToken: RefreshToken) {
        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken.memberId)
            throw RefreshTokenExpiredException(refreshToken.memberId, refreshToken.expirationDate.toString())
        }
    }
}
