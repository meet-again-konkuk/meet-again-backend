package com.konkuk.ma.domain.withdrawal.domain.policy

import java.time.LocalDate
import java.time.LocalDateTime

object WithdrawalPolicy {
    private const val GRACE_PERIOD_DAYS = 7L

    fun expirationCutoff(baseDate: LocalDate): LocalDateTime =
        baseDate.minusDays(GRACE_PERIOD_DAYS).atStartOfDay()
}
