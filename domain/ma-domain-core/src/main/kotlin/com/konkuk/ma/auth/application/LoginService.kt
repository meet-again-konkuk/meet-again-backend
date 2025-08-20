package com.konkuk.ma.auth.application

import com.konkuk.ma.auth.application.command.LoginCommand
import com.konkuk.ma.auth.domain.LoginInfo
import com.konkuk.ma.auth.domain.RefreshTokenGenerator
import com.konkuk.ma.auth.domain.port.TokenGenerator
import com.konkuk.ma.member.domain.port.MemberQueryRepository
import com.konkuk.ma.member.domain.port.PasswordEncryptor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class LoginService(
    private val memberQueryRepository: MemberQueryRepository,
    private val passwordEncryptor: PasswordEncryptor,
    private val tokenGenerator: TokenGenerator,
    private val refreshTokenGenerator: RefreshTokenGenerator
) {
    fun login(loginCommand: LoginCommand): LoginInfo {
        val member = memberQueryRepository.findByEmail(loginCommand.email)
        member.matches(loginCommand.password, passwordEncryptor)

        val accessToken = tokenGenerator.generateAccessToken(member.email)
        val refreshToken = refreshTokenGenerator.generate(member.email)

        return LoginInfo(
            accessToken = accessToken,
            refreshToken = refreshToken,
            email = member.email,
            nickname = member.nickname
        )
    }
}
