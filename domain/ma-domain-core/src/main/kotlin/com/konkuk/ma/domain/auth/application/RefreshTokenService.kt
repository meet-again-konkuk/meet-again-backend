package com.konkuk.ma.domain.auth.application

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.auth.domain.LoginInfo
import com.konkuk.ma.domain.auth.domain.RefreshTokenGenerator
import com.konkuk.ma.domain.auth.domain.RefreshTokenValidator
import com.konkuk.ma.domain.auth.domain.port.RefreshTokenRepository
import com.konkuk.ma.domain.auth.domain.port.TokenManager
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RefreshTokenService(
    private val tokenManager: TokenManager,

    private val refreshTokenRepository: RefreshTokenRepository,

    private val refreshTokenValidator: RefreshTokenValidator,

    private val refreshTokenGenerator: RefreshTokenGenerator,

    private val memberQueryRepository: MemberQueryRepository
) {
    fun refreshToken(inputRefreshToken: String): LoginInfo {
        val email = Email(tokenManager.getEmailFromToken(inputRefreshToken))
        val refreshToken = refreshTokenRepository.findOne(email)
        refreshTokenValidator.validate(refreshToken)
        val accessToken = tokenManager.generateAccessToken(email)
        val newRefreshToken = refreshTokenGenerator.generate(refreshToken.email)
        val member = memberQueryRepository.findOne(email)
        return LoginInfo(
            email, member.nickname, accessToken, newRefreshToken
        )
    }
}
