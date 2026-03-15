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
        .atTime(SHOWING_START_HOUR, 0)
        .plusDays(SHOWING_EXPIRY_DAYS),
    val matchingExpiryDate: LocalDateTime = LocalDateTime.now()
        .plusDays(MATCHING_EXPIRY_DAYS),
) {

    companion object {
        private const val SHOWING_EXPIRY_DAYS = 30L
        private const val MATCHING_EXPIRY_DAYS = 210L
        private const val SHOWING_START_HOUR = 11
    }
    fun uniqueKey(): Triple<String, Long, String> {
        return Triple(registerEmail, targetInfoId, targetEmail)
    }

    fun getRemainingDays(): Long {
        val now = LocalDate.now()
        return ChronoUnit.DAYS.between(now, showingExpiryDate)
            .coerceAtLeast(0)
    }
}
