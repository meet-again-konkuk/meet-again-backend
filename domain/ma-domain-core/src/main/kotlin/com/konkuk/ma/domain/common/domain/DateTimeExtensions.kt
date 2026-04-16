package com.konkuk.ma.domain.common.domain

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

fun LocalDateTime.hasElapsed(amount: Long, unit: ChronoUnit): Boolean {
    return this.plus(amount, unit).isBefore(LocalDateTime.now())
}

fun LocalDateTime.isExpired(): Boolean {
    return !LocalDateTime.now().isBefore(this)
}

fun LocalDateTime.remainingDays(): Long {
    return ChronoUnit.DAYS.between(LocalDateTime.now(), this).coerceAtLeast(0)
}
