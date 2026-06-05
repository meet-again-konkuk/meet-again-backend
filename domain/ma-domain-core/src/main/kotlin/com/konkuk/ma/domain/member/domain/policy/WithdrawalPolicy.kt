package com.konkuk.ma.domain.member.domain.policy

import java.time.LocalDateTime

object WithdrawalPolicy {
    const val GRACE_PERIOD_DAYS = 7L

    fun expiresAt(requestedAt: LocalDateTime): LocalDateTime {
        return requestedAt.plusDays(GRACE_PERIOD_DAYS)
    }

    fun isExpired(requestedAt: LocalDateTime, now: LocalDateTime): Boolean {
        return !now.isBefore(expiresAt(requestedAt))
    }
}
