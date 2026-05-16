package com.konkuk.ma.domain.auth.api.response

import com.konkuk.ma.domain.auth.domain.LoginInfo
import java.time.LocalDateTime

class LoginResponse(
    val status: LoginStatus,
    val email: String,
    val nickname: String?,
    val accessToken: String,
    val refreshToken: String,
    val withdrawalRequestedAt: LocalDateTime? = null,
    val withdrawalExpiresAt: LocalDateTime? = null
) {
    companion object {
        fun from(loginInfo: LoginInfo): LoginResponse = when (loginInfo) {
            is LoginInfo.Active -> LoginResponse(
                status = LoginStatus.ACTIVE,
                email = loginInfo.email.value,
                nickname = loginInfo.nickname,
                accessToken = loginInfo.accessToken,
                refreshToken = loginInfo.refreshToken.token
            )
            is LoginInfo.WithdrawalPending -> LoginResponse(
                status = LoginStatus.WITHDRAWAL_PENDING,
                email = loginInfo.email.value,
                nickname = null,
                accessToken = loginInfo.accessToken,
                refreshToken = loginInfo.refreshToken.token,
                withdrawalRequestedAt = loginInfo.withdrawalRequestedAt,
                withdrawalExpiresAt = loginInfo.withdrawalExpiresAt
            )
        }
    }
}
