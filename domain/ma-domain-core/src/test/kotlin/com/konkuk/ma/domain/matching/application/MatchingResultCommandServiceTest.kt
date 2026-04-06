package com.konkuk.ma.domain.matching.application

import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.domain.matching.exception.MatchingResultAccessDeniedException
import com.konkuk.ma.domain.matching.fixture.MatchingResultFixture
import com.konkuk.ma.exception.EntityNotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class MatchingResultCommandServiceTest : FunSpec({

    val matchingResultRepository = mockk<MatchingResultRepository>(relaxUnitFun = true)
    val service = MatchingResultCommandService(matchingResultRepository)

    beforeEach {
        clearAllMocks()
    }

    context("exclude") {

        test("매칭 결과를 제외 처리한다") {
            // Given
            val matchingResultId = 1L
            val email = "owner@example.com"
            val matchingResult = MatchingResultFixture.create(registerEmail = email)

            every { matchingResultRepository.findById(matchingResultId) } returns matchingResult

            // When
            service.exclude(matchingResultId, email)

            // Then
            matchingResult.excluded shouldBe true
            verify { matchingResultRepository.updateExcluded(matchingResult) }
        }

        test("소유권이 없는 경우 MatchingResultAccessDeniedException이 발생한다") {
            // Given
            val matchingResultId = 1L
            val ownerEmail = "owner@example.com"
            val otherEmail = "other@example.com"
            val matchingResult = MatchingResultFixture.create(registerEmail = ownerEmail)

            every { matchingResultRepository.findById(matchingResultId) } returns matchingResult

            // When & Then
            shouldThrow<MatchingResultAccessDeniedException> {
                service.exclude(matchingResultId, otherEmail)
            }.message shouldBe "매칭 결과에 대한 접근 권한이 없습니다."
        }

        test("존재하지 않는 ID이면 EntityNotFoundException이 발생한다") {
            // Given
            val nonExistentId = 999L
            val email = "owner@example.com"

            every { matchingResultRepository.findById(nonExistentId) } throws EntityNotFoundException("MatchingResult", "id", nonExistentId.toString())

            // When & Then
            shouldThrow<EntityNotFoundException> {
                service.exclude(nonExistentId, email)
            }.message shouldBe "MatchingResult을(를) 찾을 수 없습니다."
        }
    }

    context("include") {

        test("제외된 매칭 결과를 해제 처리한다") {
            // Given
            val matchingResultId = 1L
            val email = "owner@example.com"
            val matchingResult = MatchingResultFixture.create(registerEmail = email, excluded = true)

            every { matchingResultRepository.findById(matchingResultId) } returns matchingResult

            // When
            service.include(matchingResultId, email)

            // Then
            matchingResult.excluded shouldBe false
            verify { matchingResultRepository.updateExcluded(matchingResult) }
        }

        test("소유권이 없는 경우 MatchingResultAccessDeniedException이 발생한다") {
            // Given
            val matchingResultId = 1L
            val ownerEmail = "owner@example.com"
            val otherEmail = "other@example.com"
            val matchingResult = MatchingResultFixture.create(registerEmail = ownerEmail, excluded = true)

            every { matchingResultRepository.findById(matchingResultId) } returns matchingResult

            // When & Then
            shouldThrow<MatchingResultAccessDeniedException> {
                service.include(matchingResultId, otherEmail)
            }.message shouldBe "매칭 결과에 대한 접근 권한이 없습니다."
        }
    }
})
