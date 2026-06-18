package com.konkuk.ma.domain.auth.domain

import com.konkuk.ma.domain.auth.domain.port.RefreshTokenRepository
import com.konkuk.ma.domain.auth.domain.port.TokenManager
import org.springframework.stereotype.Component

@Component
class RefreshTokenGenerator(
    private val tokenManager: TokenManager,

    private val refreshTokenRepository: RefreshTokenRepository
) {
    fun generate(memberId: Long): RefreshToken {
        refreshTokenRepository.delete(memberId)
        val refreshToken = tokenManager.generateRefreshToken(memberId)
        refreshTokenRepository.save(refreshToken)
        return refreshToken
    }
}
