package com.konkuk.ma.domain.common.domain.date

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

const val ONE_DAY = 1L
const val ONE_HOUR = 1L
const val ONE_WEEK = 7L

fun LocalDateTime.hasElapsed(amount: Long, unit: ChronoUnit): Boolean {
    return this.plus(amount, unit).isBefore(LocalDateTime.now())
}

fun LocalDateTime.isExpired(): Boolean {
    return !LocalDateTime.now().isBefore(this)
}

fun LocalDateTime.remainingDays(): Long {
    return ChronoUnit.DAYS.between(LocalDate.now(), this)
        .coerceAtLeast(0)
}
