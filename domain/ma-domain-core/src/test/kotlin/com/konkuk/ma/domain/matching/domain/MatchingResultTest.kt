package com.konkuk.ma.domain.matching.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.LocalDateTime

class MatchingResultTest : FunSpec({

    context("MatchingResult.getRemainingDays") {

        test("만료일이 30일 후면 30일 반환") {
            val expiryDate = LocalDate.now().atTime(11, 0).plusDays(30)
            val result = MatchingResult(
                registerEmail = "register@example.com",
                targetInfoId = 1L,
                targetEmail = "target@example.com",
                middleNumberMatched = true,
                lastNumberMatched = true,
                yearMatched = true,
                monthMatched = true,
                dayMatched = true,
                regionMatched = true,
                showingExpiryDate = expiryDate,
                matchingExpiryDate = LocalDate.now().plusDays(210)
            )

            val remainingDays = result.getRemainingDays()

            remainingDays shouldBe 30
        }

        test("만료일이 지났으면 0 반환") {
            val result = MatchingResult(
                registerEmail = "register@example.com",
                targetInfoId = 1L,
                targetEmail = "target@example.com",
                middleNumberMatched = true,
                lastNumberMatched = true,
                yearMatched = true,
                monthMatched = true,
                dayMatched = true,
                regionMatched = true,
                showingExpiryDate = LocalDateTime.now().minusDays(1),
                matchingExpiryDate = LocalDate.now().plusDays(210)
            )

            val remainingDays = result.getRemainingDays()

            remainingDays shouldBe 0
        }

        test("만료일이 오늘이면 0 반환") {
            val result = MatchingResult(
                registerEmail = "register@example.com",
                targetInfoId = 1L,
                targetEmail = "target@example.com",
                middleNumberMatched = true,
                lastNumberMatched = true,
                yearMatched = true,
                monthMatched = true,
                dayMatched = true,
                regionMatched = true,
                showingExpiryDate = LocalDateTime.now(),
                matchingExpiryDate = LocalDate.now().plusDays(210)
            )

            val remainingDays = result.getRemainingDays()

            remainingDays shouldBe 0
        }

        test("기본 생성시 남은 일수는 0 이상") {
            val result = MatchingResult(
                registerEmail = "register@example.com",
                targetInfoId = 1L,
                targetEmail = "target@example.com",
                middleNumberMatched = true,
                lastNumberMatched = true,
                yearMatched = true,
                monthMatched = true,
                dayMatched = true,
                regionMatched = true
            )

            result.getRemainingDays() shouldBeGreaterThanOrEqual 0
        }
    }
})
