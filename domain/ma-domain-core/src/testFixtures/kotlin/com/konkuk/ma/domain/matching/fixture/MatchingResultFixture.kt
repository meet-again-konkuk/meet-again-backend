package com.konkuk.ma.domain.matching.fixture

import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.domain.NewMatchingResult
import java.time.LocalDate
import java.time.LocalDateTime

object MatchingResultFixture {
    fun create(
        id: Long = 1L,
        registerId: Long = 1L,
        targetInfoId: Long = 1L,
        targetId: Long = 2L,
        middleNumberMatched: Boolean = true,
        lastNumberMatched: Boolean = true,
        yearMatched: Boolean = true,
        monthMatched: Boolean = true,
        dayMatched: Boolean = true,
        regionMatched: Boolean = true,
        showingExpiryDate: LocalDateTime = LocalDateTime.now().plusDays(30),
        matchingExpiryDate: LocalDate = LocalDate.now().plusDays(210),
        excluded: Boolean = false,
        claimed: Boolean = false,
    ): MatchingResult {
        return MatchingResult(
            id = id,
            registerId = registerId,
            targetInfoId = targetInfoId,
            targetId = targetId,
            middleNumberMatched = middleNumberMatched,
            lastNumberMatched = lastNumberMatched,
            yearMatched = yearMatched,
            monthMatched = monthMatched,
            dayMatched = dayMatched,
            regionMatched = regionMatched,
            showingExpiryDate = showingExpiryDate,
            matchingExpiryDate = matchingExpiryDate,
            excluded = excluded,
            claimed = claimed,
        )
    }
}

object NewMatchingResultFixture {
    fun create(
        registerId: Long = 1L,
        targetInfoId: Long = 1L,
        targetId: Long = 2L,
        middleNumberMatched: Boolean = true,
        lastNumberMatched: Boolean = true,
        yearMatched: Boolean = true,
        monthMatched: Boolean = true,
        dayMatched: Boolean = true,
        regionMatched: Boolean = true,
        showingExpiryDate: LocalDateTime = LocalDateTime.now().plusDays(30),
        matchingExpiryDate: LocalDate = LocalDate.now().plusDays(210),
    ): NewMatchingResult {
        return NewMatchingResult(
            registerId = registerId,
            targetInfoId = targetInfoId,
            targetId = targetId,
            middleNumberMatched = middleNumberMatched,
            lastNumberMatched = lastNumberMatched,
            yearMatched = yearMatched,
            monthMatched = monthMatched,
            dayMatched = dayMatched,
            regionMatched = regionMatched,
            showingExpiryDate = showingExpiryDate,
            matchingExpiryDate = matchingExpiryDate,
        )
    }
}
