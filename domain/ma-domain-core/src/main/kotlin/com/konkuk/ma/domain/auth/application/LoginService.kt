package com.konkuk.ma.domain.auth.application

import com.konkuk.ma.domain.auth.application.command.LoginCommand
import com.konkuk.ma.domain.auth.domain.AuthTokenIssuer
import com.konkuk.ma.domain.auth.domain.LoginInfo
import com.konkuk.ma.domain.auth.domain.PasswordVerifier
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class LoginService(
    private val memberQueryRepository: MemberQueryRepository,
    private val passwordVerifier: PasswordVerifier,
    private val authTokenIssuer: AuthTokenIssuer,
) {
    fun login(loginCommand: LoginCommand): LoginInfo {
        val member = memberQueryRepository.findOne(loginCommand.email)
        passwordVerifier.verify(loginCommand.password, member)

        val authTokens = authTokenIssuer.issueFor(member.email)
        return LoginInfo.from(member, authTokens, LocalDateTime.now())
    }
}
