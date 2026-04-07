package com.konkuk.ma.domain.auth.application

import com.konkuk.ma.domain.auth.application.command.LoginCommand
import com.konkuk.ma.domain.auth.domain.LoginInfo
import com.konkuk.ma.domain.auth.domain.PasswordVerifier
import com.konkuk.ma.domain.auth.domain.RefreshTokenGenerator
import com.konkuk.ma.domain.auth.domain.port.TokenManager
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class LoginService(
    private val memberQueryRepository: MemberQueryRepository,
    private val passwordVerifier: PasswordVerifier,
    private val tokenManager: TokenManager,
    private val refreshTokenGenerator: RefreshTokenGenerator
) {
    fun login(loginCommand: LoginCommand): LoginInfo {
        val member = memberQueryRepository.findOne(loginCommand.email)
        passwordVerifier.verify(loginCommand.password, member)

        val accessToken = tokenManager.generateAccessToken(member.email)
        val refreshToken = refreshTokenGenerator.generate(member.email)

        return LoginInfo(
            accessToken = accessToken,
            refreshToken = refreshToken,
            email = member.email,
            nickname = member.nickname
        )
    }
}
