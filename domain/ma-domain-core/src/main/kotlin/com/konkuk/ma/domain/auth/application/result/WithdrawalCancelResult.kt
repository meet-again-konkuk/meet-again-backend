package com.konkuk.ma.domain.auth.application.result

import com.konkuk.ma.domain.auth.domain.LoginInfo
import java.time.LocalDateTime

class WithdrawalCancelResult(
    val loginInfo: LoginInfo,
    val cancelledAt: LocalDateTime
)
