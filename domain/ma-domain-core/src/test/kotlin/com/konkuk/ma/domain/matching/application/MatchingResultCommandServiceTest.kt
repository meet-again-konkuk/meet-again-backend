package com.konkuk.ma.domain.matching.application

import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.domain.matching.fixture.MatchingResultFixture
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
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
            val memberId = 1L
            val matchingResult = MatchingResultFixture.create(registerId = memberId)

            every { matchingResultRepository.findOne(matchingResultId) } returns matchingResult

            // When
            service.exclude(matchingResultId, memberId)

            // Then
            matchingResult.excluded shouldBe true
            verify { matchingResultRepository.updateExcluded(matchingResult) }
        }

        test("소유권이 없는 경우 AccessDeniedException이 발생한다") {
            // Given
            val matchingResultId = 1L
            val ownerId = 1L
            val otherId = 2L
            val matchingResult = MatchingResultFixture.create(registerId = ownerId)

            every { matchingResultRepository.findOne(matchingResultId) } returns matchingResult

            // When & Then
            shouldThrow<AccessDeniedException> {
                service.exclude(matchingResultId, otherId)
            }
        }

        test("존재하지 않는 ID이면 EntityNotFoundException이 발생한다") {
            // Given
            val nonExistentId = 999L
            val memberId = 1L

            every { matchingResultRepository.findOne(nonExistentId) } throws EntityNotFoundException(EntityType.MATCHING_RESULT, nonExistentId.toString())

            // When & Then
            shouldThrow<EntityNotFoundException> {
                service.exclude(nonExistentId, memberId)
            }
        }
    }

    context("include") {

        test("제외된 매칭 결과를 해제 처리한다") {
            // Given
            val matchingResultId = 1L
            val memberId = 1L
            val matchingResult = MatchingResultFixture.create(registerId = memberId, excluded = true)

            every { matchingResultRepository.findOne(matchingResultId) } returns matchingResult

            // When
            service.include(matchingResultId, memberId)

            // Then
            matchingResult.excluded shouldBe false
            verify { matchingResultRepository.updateExcluded(matchingResult) }
        }

        test("소유권이 없는 경우 AccessDeniedException이 발생한다") {
            // Given
            val matchingResultId = 1L
            val ownerId = 1L
            val otherId = 2L
            val matchingResult = MatchingResultFixture.create(registerId = ownerId, excluded = true)

            every { matchingResultRepository.findOne(matchingResultId) } returns matchingResult

            // When & Then
            shouldThrow<AccessDeniedException> {
                service.include(matchingResultId, otherId)
            }
        }
    }
})
