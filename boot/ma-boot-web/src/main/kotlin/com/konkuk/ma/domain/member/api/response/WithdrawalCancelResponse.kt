package com.konkuk.ma.domain.member.api.response

import com.konkuk.ma.domain.auth.application.result.WithdrawalCancelResult
import java.time.LocalDateTime

class WithdrawalCancelResponse(
    val email: String,
    val nickname: String,
    val cancelledAt: LocalDateTime,
    val accessToken: String,
    val refreshToken: String
) {
    companion object {
        fun from(result: WithdrawalCancelResult): WithdrawalCancelResponse {
            val loginInfo = result.loginInfo
            return WithdrawalCancelResponse(
                email = loginInfo.email.value,
                nickname = loginInfo.nickname,
                cancelledAt = result.cancelledAt,
                accessToken = loginInfo.accessToken,
                refreshToken = loginInfo.refreshToken.token
            )
        }
    }
}
