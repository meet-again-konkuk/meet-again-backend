package com.konkuk.ma.domain.auth.domain

import com.konkuk.ma.domain.auth.domain.port.TokenManager
import com.konkuk.ma.domain.common.domain.Email
import org.springframework.stereotype.Component

@Component
class AuthTokenIssuer(
    private val tokenManager: TokenManager,
    private val refreshTokenGenerator: RefreshTokenGenerator,
) {
    fun issueFor(email: Email): AuthTokens {
        val accessToken = tokenManager.generateAccessToken(email)
        val refreshToken = refreshTokenGenerator.generate(email)
        return AuthTokens(accessToken, refreshToken)
    }
}
