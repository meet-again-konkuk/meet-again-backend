package com.konkuk.ma.auth.application

import com.konkuk.ma.auth.domain.LoginInfo
import com.konkuk.ma.auth.domain.RefreshTokenGenerator
import com.konkuk.ma.auth.domain.RefreshTokenValidator
import com.konkuk.ma.auth.domain.port.RefreshTokenRepository
import com.konkuk.ma.auth.domain.port.TokenGenerator
import com.konkuk.ma.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RefreshTokenService(
    private val tokenGenerator: TokenGenerator,

    private val refreshTokenRepository: RefreshTokenRepository,

    private val refreshTokenValidator: RefreshTokenValidator,

    private val refreshTokenGenerator: RefreshTokenGenerator,

    private val memberQueryRepository: MemberQueryRepository
) {
    fun refreshToken(inputRefreshToken: String): LoginInfo {
        val email = tokenGenerator.getEmailFromToken(inputRefreshToken)
        val refreshToken = refreshTokenRepository.findByEmail(email)
        refreshTokenValidator.validate(refreshToken)
        val accessToken = tokenGenerator.generateAccessToken(email)
        val newRefreshToken = refreshTokenGenerator.generate(refreshToken.email)
        val member = memberQueryRepository.findByEmail(email)
        return LoginInfo(
            email, member.nickname, accessToken, newRefreshToken
        )
    }
}
