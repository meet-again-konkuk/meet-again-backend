package com.konkuk.ma.domain.auth.domain

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.domain.Member
import java.time.LocalDateTime

sealed class LoginInfo {
    abstract val email: Email
    abstract val accessToken: String
    abstract val refreshToken: RefreshToken

    class Active(
        override val email: Email,
        val nickname: String,
        override val accessToken: String,
        override val refreshToken: RefreshToken
    ) : LoginInfo() {
        companion object {
            fun from(member: Member, authTokens: AuthTokens): Active = Active(
                email = member.email,
                nickname = member.nickname,
                accessToken = authTokens.accessToken,
                refreshToken = authTokens.refreshToken
            )
        }
    }

    class WithdrawalPending(
        override val email: Email,
        override val accessToken: String,
        override val refreshToken: RefreshToken,
        val withdrawalRequestedAt: LocalDateTime,
        val withdrawalExpiresAt: LocalDateTime
    ) : LoginInfo()

    companion object {
        fun from(member: Member, authTokens: AuthTokens, now: LocalDateTime): LoginInfo {
            val graceWindow = member.withdrawalGraceWindowOrNull(now)
            if (graceWindow != null) {
                return WithdrawalPending(
                    email = member.email,
                    accessToken = authTokens.accessToken,
                    refreshToken = authTokens.refreshToken,
                    withdrawalRequestedAt = graceWindow.requestedAt,
                    withdrawalExpiresAt = graceWindow.expiresAt
                )
            }
            return Active.from(member, authTokens)
        }
    }
}
