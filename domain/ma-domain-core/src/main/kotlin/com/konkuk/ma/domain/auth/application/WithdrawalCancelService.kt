package com.konkuk.ma.domain.auth.application

import com.konkuk.ma.domain.auth.application.result.WithdrawalCancelResult
import com.konkuk.ma.domain.auth.domain.AuthTokenIssuer
import com.konkuk.ma.domain.auth.domain.LoginInfo
import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.domain.port.MemberCommandRepository
import com.konkuk.ma.domain.member.domain.port.MemberQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class WithdrawalCancelService(
    private val memberQueryRepository: MemberQueryRepository,
    private val memberCommandRepository: MemberCommandRepository,
    private val authTokenIssuer: AuthTokenIssuer,
) {
    fun cancel(email: String): WithdrawalCancelResult {
        val member = memberQueryRepository.findOne(Email(email))
        val now = LocalDateTime.now()
        member.cancelWithdrawal(now)
        memberCommandRepository.cancelWithdrawal(member.email)

        val authTokens = authTokenIssuer.issueFor(member.email)
        val loginInfo = LoginInfo(member.email, member.nickname, authTokens.accessToken, authTokens.refreshToken)
        return WithdrawalCancelResult(loginInfo, now)
    }
}
