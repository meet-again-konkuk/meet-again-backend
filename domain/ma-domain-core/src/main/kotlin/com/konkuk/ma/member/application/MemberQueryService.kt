package com.konkuk.ma.member.application

import com.konkuk.ma.member.application.command.LoginCommand
import com.konkuk.ma.member.domain.port.MemberQueryRepository
import com.konkuk.ma.member.domain.port.PasswordEncryptor
import com.konkuk.ma.member.domain.port.TokenGenerator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberQueryService(
    private val memberQueryRepository: MemberQueryRepository,
    private val passwordEncryptor: PasswordEncryptor,
    private val tokenGenerator: TokenGenerator
) {
    fun checkDuplicatedNickname(nickname: String): Boolean {
        return memberQueryRepository.existsByNickname(nickname)
    }

    fun checkDuplicatedEmail(email: String): Boolean {
        return memberQueryRepository.existsByEmail(email)
    }

    fun login(loginCommand: LoginCommand): LoginResult {
        val member = memberQueryRepository.findByEmail(loginCommand.email)
        member.matches(loginCommand.password, passwordEncryptor)

        val accessToken = tokenGenerator.generateAccessToken(member.email)
        val refreshToken = tokenGenerator.generateRefreshToken(member.email)

        return LoginResult(
            accessToken = accessToken,
            refreshToken = refreshToken,
            email = member.email,
            nickname = member.nickname
        )
    }
}

data class LoginResult(
    val accessToken: String,
    val refreshToken: String,
    val email: String,
    val nickname: String
)
