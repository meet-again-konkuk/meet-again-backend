package com.konkuk.ma.job.fixture

import com.konkuk.ma.domain.matching.domain.MatchingResult
import java.time.LocalDate
import java.time.LocalDateTime

object MatchingResultFixture {
    fun create(
        registerEmail: String = "register@example.com",
        targetInfoId: Long = 1L,
        targetEmail: String = "target@example.com",
        middleNumberMatched: Boolean = true,
        lastNumberMatched: Boolean = true,
        yearMatched: Boolean = true,
        monthMatched: Boolean = true,
        dayMatched: Boolean = true,
        regionMatched: Boolean = true,
        showingExpiryDate: LocalDateTime = LocalDate.now().atTime(11, 0).plusDays(30),
        matchingExpiryDate: LocalDate = LocalDate.now().plusDays(210)
    ): MatchingResult {
        return MatchingResult(
            registerEmail = registerEmail,
            targetInfoId = targetInfoId,
            targetEmail = targetEmail,
            middleNumberMatched = middleNumberMatched,
            lastNumberMatched = lastNumberMatched,
            yearMatched = yearMatched,
            monthMatched = monthMatched,
            dayMatched = dayMatched,
            regionMatched = regionMatched,
            showingExpiryDate = showingExpiryDate,
            matchingExpiryDate = matchingExpiryDate
        )
    }
}
