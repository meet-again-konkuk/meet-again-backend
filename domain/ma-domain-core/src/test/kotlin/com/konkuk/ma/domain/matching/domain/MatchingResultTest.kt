package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.matching.fixture.MatchingResultFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.LocalDateTime

class MatchingResultTest : FunSpec({

    context("MatchingResult.getRemainingDays") {

        test("만료일이 30일 후면 30일 반환") {
            // Given
            val expiryDate = LocalDate.now().atTime(11, 0).plusDays(30)
            val result = MatchingResultFixture.create(showingExpiryDate = expiryDate)

            // When
            val remainingDays = result.getRemainingDays()

            // Then
            remainingDays shouldBe 30
        }

        test("만료일이 지났으면 0 반환") {
            // Given
            val result = MatchingResultFixture.create(
                showingExpiryDate = LocalDateTime.now().minusDays(1)
            )

            // When
            val remainingDays = result.getRemainingDays()

            // Then
            remainingDays shouldBe 0
        }

        test("만료일이 오늘이면 0 반환") {
            // Given
            val result = MatchingResultFixture.create(
                showingExpiryDate = LocalDateTime.now()
            )

            // When
            val remainingDays = result.getRemainingDays()

            // Then
            remainingDays shouldBe 0
        }

        test("기본 생성시 남은 일수는 0 이상") {
            // Given
            val result = MatchingResultFixture.create()

            // When & Then
            result.getRemainingDays() shouldBeGreaterThanOrEqual 0
        }
    }
})
