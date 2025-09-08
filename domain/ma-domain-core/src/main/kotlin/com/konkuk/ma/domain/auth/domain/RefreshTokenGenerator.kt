package com.konkuk.ma.domain.auth.domain

import com.konkuk.ma.domain.auth.domain.port.RefreshTokenRepository
import com.konkuk.ma.domain.auth.domain.port.TokenManager
import org.springframework.stereotype.Component

@Component
class RefreshTokenGenerator(
    private val tokenManager: TokenManager,

    private val refreshTokenRepository: RefreshTokenRepository
) {
    fun generate(email: String): RefreshToken {
        refreshTokenRepository.delete(email)
        val refreshToken = tokenManager.generateRefreshToken(email)
        refreshTokenRepository.save(refreshToken)
        return refreshToken
    }
}
