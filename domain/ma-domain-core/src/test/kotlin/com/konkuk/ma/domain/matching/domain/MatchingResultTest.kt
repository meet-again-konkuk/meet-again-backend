package com.konkuk.ma.domain.matching.domain

import com.konkuk.ma.domain.matching.fixture.MatchingResultFixture
import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.InvalidStateException
import io.kotest.assertions.throwables.shouldThrow
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

    context("validateOwnership") {

        test("본인이면 예외 없이 통과한다") {
            val matchingResult = MatchingResultFixture.create(registerId = 1L)

            matchingResult.validateOwnership(1L)
        }

        test("다른 회원이면 AccessDeniedException이 발생한다") {
            val matchingResult = MatchingResultFixture.create(registerId = 1L)

            shouldThrow<AccessDeniedException> {
                matchingResult.validateOwnership(2L)
            }
        }
    }

    context("isVisible") {

        test("공개 시작 시각이 지났으면 true를 반환한다") {
            val matchingResult = MatchingResultFixture.create(
                showingExpiryDate = LocalDateTime.now().plusDays(29),
            )

            matchingResult.isVisible() shouldBe true
        }

        test("공개 시작 시각 전이면 false를 반환한다") {
            val matchingResult = MatchingResultFixture.create(
                showingExpiryDate = LocalDateTime.now().plusDays(31),
            )

            matchingResult.isVisible() shouldBe false
        }
    }

    context("exclude / include") {

        test("exclude 호출 시 excluded가 true가 된다") {
            val matchingResult = MatchingResultFixture.create()

            matchingResult.exclude()

            matchingResult.excluded shouldBe true
        }

        test("include 호출 시 excluded가 false가 된다") {
            val matchingResult = MatchingResultFixture.create(excluded = true)

            matchingResult.include()

            matchingResult.excluded shouldBe false
        }
    }

    context("claim") {

        test("claim 호출 시 claimed가 true가 된다") {
            val matchingResult = MatchingResultFixture.create()

            matchingResult.claim()

            matchingResult.claimed shouldBe true
        }

        test("이미 claimed된 상태에서 claim하면 예외가 발생한다") {
            val matchingResult = MatchingResultFixture.create(claimed = true)

            shouldThrow<InvalidStateException> {
                matchingResult.claim()
            }
        }
    }

    context("MatchingResult.calculateMatchRate") {

        test("전화번호 완전 + 생년월일 완전 + 지역 일치 시 99% 반환") {
            val result = MatchingResultFixture.create(
                middleNumberMatched = true,
                lastNumberMatched = true,
                yearMatched = true,
                monthMatched = true,
                dayMatched = true,
                regionMatched = true,
            )

            result.matchRate shouldBe 99
        }

        test("전화번호 완전 + 생년월일 완전 일치 시 97% 반환") {
            val result = MatchingResultFixture.create(
                middleNumberMatched = true,
                lastNumberMatched = true,
                yearMatched = true,
                monthMatched = true,
                dayMatched = true,
                regionMatched = false,
            )

            result.matchRate shouldBe 97
        }

        test("전화번호 완전 + 지역 일치 시 90% 반환") {
            val result = MatchingResultFixture.create(
                middleNumberMatched = true,
                lastNumberMatched = true,
                yearMatched = false,
                monthMatched = false,
                dayMatched = false,
                regionMatched = true,
            )

            result.matchRate shouldBe 90
        }

        test("전화번호 완전 일치만 있으면 85% 반환") {
            val result = MatchingResultFixture.create(
                middleNumberMatched = true,
                lastNumberMatched = true,
                yearMatched = false,
                monthMatched = false,
                dayMatched = false,
                regionMatched = false,
            )

            result.matchRate shouldBe 85
        }

        test("생년월일 완전 + 지역 일치 시 83% 반환") {
            val result = MatchingResultFixture.create(
                middleNumberMatched = false,
                lastNumberMatched = false,
                yearMatched = true,
                monthMatched = true,
                dayMatched = true,
                regionMatched = true,
            )

            result.matchRate shouldBe 83
        }

        test("생년월일 완전 일치만 있으면 80% 반환") {
            val result = MatchingResultFixture.create(
                middleNumberMatched = false,
                lastNumberMatched = false,
                yearMatched = true,
                monthMatched = true,
                dayMatched = true,
                regionMatched = false,
            )

            result.matchRate shouldBe 80
        }

        test("전화번호 부분(중간번호만) + 지역 일치 시 40% 반환") {
            val result = MatchingResultFixture.create(
                middleNumberMatched = true,
                lastNumberMatched = false,
                yearMatched = false,
                monthMatched = false,
                dayMatched = false,
                regionMatched = true,
            )

            result.matchRate shouldBe 40
        }

        test("생년월일 부분(연도+월만) 일치 시 20% 반환") {
            val result = MatchingResultFixture.create(
                middleNumberMatched = false,
                lastNumberMatched = false,
                yearMatched = true,
                monthMatched = true,
                dayMatched = false,
                regionMatched = false,
            )

            result.matchRate shouldBe 20
        }

        test("아무것도 일치하지 않으면 0% 반환") {
            val result = MatchingResultFixture.create(
                middleNumberMatched = false,
                lastNumberMatched = false,
                yearMatched = false,
                monthMatched = false,
                dayMatched = false,
                regionMatched = false,
            )

            result.matchRate shouldBe 0
        }

        test("전화번호 부분 + 생년월일 부분(연도만) + 지역 일치 시 50% 반환") {
            val result = MatchingResultFixture.create(
                middleNumberMatched = true,
                lastNumberMatched = false,
                yearMatched = true,
                monthMatched = false,
                dayMatched = false,
                regionMatched = true,
            )

            result.matchRate shouldBe 50
        }
    }
})
