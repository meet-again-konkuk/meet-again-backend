package com.konkuk.ma.domain.matching.fixture

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.matching.domain.MatchingResult
import com.konkuk.ma.domain.matching.domain.NewMatchingResult
import java.time.LocalDate
import java.time.LocalDateTime

object MatchingResultFixture {
    fun create(
        id: Long = 1L,
        registerEmail: String = "register@example.com",
        targetInfoId: Long = 1L,
        targetEmail: String = "target@example.com",
        middleNumberMatched: Boolean = true,
        lastNumberMatched: Boolean = true,
        yearMatched: Boolean = true,
        monthMatched: Boolean = true,
        dayMatched: Boolean = true,
        regionMatched: Boolean = true,
        showingExpiryDate: LocalDateTime = LocalDateTime.now().plusDays(30),
        matchingExpiryDate: LocalDate = LocalDate.now().plusDays(210),
        excluded: Boolean = false,
    ): MatchingResult {
        return MatchingResult(
            id = id,
            registerEmail = Email(registerEmail),
            targetInfoId = targetInfoId,
            targetEmail = Email(targetEmail),
            middleNumberMatched = middleNumberMatched,
            lastNumberMatched = lastNumberMatched,
            yearMatched = yearMatched,
            monthMatched = monthMatched,
            dayMatched = dayMatched,
            regionMatched = regionMatched,
            showingExpiryDate = showingExpiryDate,
            matchingExpiryDate = matchingExpiryDate,
            excluded = excluded,
        )
    }
}

object NewMatchingResultFixture {
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
        showingExpiryDate: LocalDateTime = LocalDateTime.now().plusDays(30),
        matchingExpiryDate: LocalDate = LocalDate.now().plusDays(210),
    ): NewMatchingResult {
        return NewMatchingResult(
            registerEmail = Email(registerEmail),
            targetInfoId = targetInfoId,
            targetEmail = Email(targetEmail),
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
