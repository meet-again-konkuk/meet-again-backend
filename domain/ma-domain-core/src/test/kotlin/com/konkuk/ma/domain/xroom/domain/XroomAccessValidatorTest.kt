package com.konkuk.ma.domain.xroom.domain

import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.domain.matching.fixture.MatchingResultFixture
import com.konkuk.ma.domain.xroom.fixture.XroomFixture
import com.konkuk.ma.exception.AccessDeniedException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime

class XroomAccessValidatorTest : FunSpec({

    val matchingResultRepository = mockk<MatchingResultRepository>()
    val xroomAccessValidator = XroomAccessValidator(matchingResultRepository)

    context("validate") {

        test("작성자이면 매칭 조회 없이 통과한다") {
            val xroom = XroomFixture.create(ownerId = 1L)

            xroomAccessValidator.validate(xroom, 1L)
        }

        test("작성자가 아니어도 claim한 매칭의 수신자이면 통과한다") {
            val xroom = XroomFixture.create(ownerId = 1L, targetInfoId = 100L)
            every { matchingResultRepository.findClaimedByTarget(2L) } returns listOf(
                MatchingResultFixture.create(
                    targetId = 2L,
                    targetInfoId = 100L,
                    claimed = true,
                    showingExpiryDate = LocalDateTime.now().plusDays(29),
                )
            )

            xroomAccessValidator.validate(xroom, 2L)
        }

        test("작성자도 수신자도 아니면 AccessDeniedException이 발생한다") {
            val xroom = XroomFixture.create(ownerId = 1L, targetInfoId = 100L)
            every { matchingResultRepository.findClaimedByTarget(3L) } returns emptyList()

            shouldThrow<AccessDeniedException> {
                xroomAccessValidator.validate(xroom, 3L)
            }
        }
    }
})
