package com.konkuk.ma.domain.member.application.result

import com.konkuk.ma.domain.auth.domain.LoginInfo
import java.time.LocalDateTime

class WithdrawalCancelResult(
    val loginInfo: LoginInfo.Active,
    val cancelledAt: LocalDateTime
)
