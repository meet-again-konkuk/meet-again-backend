package com.konkuk.ma.domain.member.domain.policy

import java.time.LocalDateTime

class WithdrawalGraceWindow(
    val requestedAt: LocalDateTime,
    val expiresAt: LocalDateTime
)
