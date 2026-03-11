package com.konkuk.ma.domain.matching.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class MatchingResult(
    val registerEmail: String,
    val targetInfoId: Long,
    val targetEmail: String,
    val middleNumberMatched: Boolean,
    val lastNumberMatched: Boolean,
    val yearMatched: Boolean,
    val monthMatched: Boolean,
    val dayMatched: Boolean,
    val regionMatched: Boolean,
    val showingExpiryDate: LocalDateTime = LocalDate.now()
        .atTime(11, 0)
        .plusDays(30),
    val matchingExpiryDate: LocalDateTime = LocalDateTime.now()
        .plusDays(210),
) {
    fun getRemainingDays(): Long {
        val now = LocalDate.now()
        return ChronoUnit.DAYS.between(now, showingExpiryDate)
            .coerceAtLeast(0)
    }
}
