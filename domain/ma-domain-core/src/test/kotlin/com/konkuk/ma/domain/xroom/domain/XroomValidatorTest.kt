package com.konkuk.ma.domain.xroom.domain

import com.konkuk.ma.domain.matching.domain.port.MatchingResultRepository
import com.konkuk.ma.domain.matching.domain.port.TargetInfoQueryRepository
import com.konkuk.ma.domain.matching.fixture.MatchingResultFixture
import com.konkuk.ma.domain.matching.fixture.TargetInfoFixture
import com.konkuk.ma.domain.xroom.domain.port.MemoryQueryRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomQueryRepository
import com.konkuk.ma.domain.xroom.fixture.MemoryFixture
import com.konkuk.ma.domain.xroom.fixture.XroomFixture
import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.DuplicateException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime

class XroomValidatorTest : FunSpec({

    val targetInfoQueryRepository = mockk<TargetInfoQueryRepository>()
    val xroomQueryRepository = mockk<XroomQueryRepository>()
    val memoryQueryRepository = mockk<MemoryQueryRepository>()
    val matchingResultRepository = mockk<MatchingResultRepository>()
    val xroomValidator =
        XroomValidator(targetInfoQueryRepository, xroomQueryRepository, memoryQueryRepository, matchingResultRepository)

    context("validate") {

        test("소유자가 일치하고 중복된 방이 없으면 예외 없이 통과한다") {
            val targetInfo = TargetInfoFixture.create(registerId = 1L)
            val newXroom = NewXroom(
                ownerId = 1L,
                targetInfoId = targetInfo.targetInfoId,
            )
            every { targetInfoQueryRepository.findOne(targetInfo.targetInfoId) } returns targetInfo
            every { xroomQueryRepository.exists(targetInfo.targetInfoId) } returns false

            xroomValidator.validate(newXroom)
        }

        test("본인 소유가 아닌 TargetInfo이면 AccessDeniedException이 발생한다") {
            val targetInfo = TargetInfoFixture.create(registerId = 1L)
            val newXroom = NewXroom(
                ownerId = 2L,
                targetInfoId = targetInfo.targetInfoId,
            )
            every { targetInfoQueryRepository.findOne(targetInfo.targetInfoId) } returns targetInfo

            shouldThrow<AccessDeniedException> {
                xroomValidator.validate(newXroom)
            }
        }

        test("이미 방이 존재하는 TargetInfo이면 DuplicateException이 발생한다") {
            val targetInfo = TargetInfoFixture.create(registerId = 1L)
            val newXroom = NewXroom(
                ownerId = 1L,
                targetInfoId = targetInfo.targetInfoId,
            )
            every { targetInfoQueryRepository.findOne(targetInfo.targetInfoId) } returns targetInfo
            every { xroomQueryRepository.exists(targetInfo.targetInfoId) } returns true

            shouldThrow<DuplicateException> {
                xroomValidator.validate(newXroom)
            }
        }
    }

    context("validateAccessible") {

        test("작성자이면 매칭 조회 없이 통과한다") {
            val xroom = XroomFixture.create(ownerId = 1L)

            xroomValidator.validateAccessible(xroom, 1L)
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

            xroomValidator.validateAccessible(xroom, 2L)
        }

        test("작성자도 수신자도 아니면 AccessDeniedException이 발생한다") {
            val xroom = XroomFixture.create(ownerId = 1L, targetInfoId = 100L)
            every { matchingResultRepository.findClaimedByTarget(3L) } returns emptyList()

            shouldThrow<AccessDeniedException> {
                xroomValidator.validateAccessible(xroom, 3L)
            }
        }

        test("claim한 매칭이 있어도 이 방의 targetInfo가 아니면 AccessDeniedException이 발생한다") {
            val xroom = XroomFixture.create(ownerId = 1L, targetInfoId = 100L)
            every { matchingResultRepository.findClaimedByTarget(2L) } returns listOf(
                MatchingResultFixture.create(
                    targetId = 2L,
                    targetInfoId = 999L,
                    claimed = true,
                    showingExpiryDate = LocalDateTime.now().plusDays(29),
                )
            )

            shouldThrow<AccessDeniedException> {
                xroomValidator.validateAccessible(xroom, 2L)
            }
        }

        test("claim했지만 아직 공개 전(isVisible=false)인 매칭은 접근을 허용하지 않는다") {
            val xroom = XroomFixture.create(ownerId = 1L, targetInfoId = 100L)
            every { matchingResultRepository.findClaimedByTarget(2L) } returns listOf(
                MatchingResultFixture.create(
                    targetId = 2L,
                    targetInfoId = 100L,
                    claimed = true,
                    showingExpiryDate = LocalDateTime.now().plusDays(31),
                )
            )

            shouldThrow<AccessDeniedException> {
                xroomValidator.validateAccessible(xroom, 2L)
            }
        }
    }

    context("validateOwnedXroom") {

        test("작성자이면 통과한다") {
            every { xroomQueryRepository.findOne(10L) } returns XroomFixture.create(id = 10L, ownerId = 1L)

            xroomValidator.validateOwnedXroom(10L, 1L)
        }

        test("작성자가 아니면 AccessDeniedException이 발생한다") {
            every { xroomQueryRepository.findOne(10L) } returns XroomFixture.create(id = 10L, ownerId = 1L)

            shouldThrow<AccessDeniedException> {
                xroomValidator.validateOwnedXroom(10L, 2L)
            }
        }
    }

    context("validateOwnedMemory") {

        test("작성자이고 기억이 해당 방에 속하면 그 기억을 반환한다") {
            every { xroomQueryRepository.findOne(10L) } returns XroomFixture.create(id = 10L, ownerId = 1L)
            every { memoryQueryRepository.findOne(20L) } returns MemoryFixture.create(id = 20L, xroomId = 10L)

            val memory = xroomValidator.validateOwnedMemory(10L, 20L, 1L)

            memory.id shouldBe 20L
        }

        test("작성자가 아니면 AccessDeniedException이 발생한다") {
            every { xroomQueryRepository.findOne(10L) } returns XroomFixture.create(id = 10L, ownerId = 1L)

            shouldThrow<AccessDeniedException> {
                xroomValidator.validateOwnedMemory(10L, 20L, 2L)
            }
        }

        test("기억이 다른 방에 속하면 AccessDeniedException이 발생한다") {
            every { xroomQueryRepository.findOne(10L) } returns XroomFixture.create(id = 10L, ownerId = 1L)
            every { memoryQueryRepository.findOne(20L) } returns MemoryFixture.create(id = 20L, xroomId = 99L)

            shouldThrow<AccessDeniedException> {
                xroomValidator.validateOwnedMemory(10L, 20L, 1L)
            }
        }
    }
})
