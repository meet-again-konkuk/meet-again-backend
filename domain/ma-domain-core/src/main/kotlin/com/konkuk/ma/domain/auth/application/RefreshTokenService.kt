package com.konkuk.ma.domain.auth.application

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
        val memberId = tokenManager.getMemberIdFromToken(inputRefreshToken)
        val refreshToken = refreshTokenRepository.findOne(memberId)
        refreshTokenValidator.validate(refreshToken)
        val member = memberQueryRepository.findOne(memberId)
        member.verifyLogin()

        val accessToken = tokenManager.generateAccessToken(memberId)
        val newRefreshToken = refreshTokenGenerator.generate(memberId)

        return LoginInfo(
            member.email, member.nickname, accessToken, newRefreshToken
        )
    }
}
