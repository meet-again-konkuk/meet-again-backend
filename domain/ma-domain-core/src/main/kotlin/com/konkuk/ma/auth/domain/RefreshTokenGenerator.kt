package com.konkuk.ma.auth.domain

import com.konkuk.ma.auth.domain.port.RefreshTokenRepository
import com.konkuk.ma.auth.domain.port.TokenGenerator
import org.springframework.stereotype.Component

@Component
class RefreshTokenGenerator(
    private val tokenGenerator: TokenGenerator,

    private val refreshTokenRepository: RefreshTokenRepository
) {
    fun generate(email: String): RefreshToken {
        refreshTokenRepository.delete(email)
        val refreshToken = tokenGenerator.generateRefreshToken(email)
        refreshTokenRepository.save(refreshToken)
        return refreshToken
    }
}
