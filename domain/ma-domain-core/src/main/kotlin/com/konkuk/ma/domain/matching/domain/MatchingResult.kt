package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.matching.exception.MatchingResultAccessDeniedException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class MatchingResult(
    val id: Long = 0L,
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
    val matchingExpiryDate: LocalDate = LocalDate.now()
        .plusDays(MATCHING_EXPIRY_DAYS),
) {
    val matchRate: Int by lazy {
        MatchRateCalculator(
            groups = listOf(
                MatchingGroup.Phone(middleNumberMatched, lastNumberMatched),
                MatchingGroup.Birth(yearMatched, monthMatched, dayMatched),
            ),
            regionMatched = regionMatched,
        ).calculate()
    }

    companion object {
        private const val SHOWING_EXPIRY_DAYS = 30L
        private const val MATCHING_EXPIRY_DAYS = 210L
        private const val SHOWING_START_HOUR = 11
    }

    fun createUniqueKey(): Pair<Long, String> {
        return Pair(targetInfoId, targetEmail)
    }

    fun getRemainingDays(): Long {
        val now = LocalDate.now()
        return ChronoUnit.DAYS.between(now, showingExpiryDate)
            .coerceAtLeast(0)
    }

    fun validateOwnership(email: String) {
        if (registerEmail != email) {
            throw MatchingResultAccessDeniedException(id, email)
        }
    }
}
