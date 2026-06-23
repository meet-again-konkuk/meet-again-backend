package com.konkuk.ma.domain.xroom.domain

import com.konkuk.ma.domain.matching.domain.port.TargetInfoQueryRepository
import com.konkuk.ma.domain.matching.fixture.TargetInfoFixture
import com.konkuk.ma.domain.xroom.domain.port.XroomQueryRepository
import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.DuplicateException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk

class XroomValidatorTest : FunSpec({

    val targetInfoQueryRepository = mockk<TargetInfoQueryRepository>()
    val xroomQueryRepository = mockk<XroomQueryRepository>()
    val xroomValidator = XroomValidator(targetInfoQueryRepository, xroomQueryRepository)

    context("validate") {

        test("소유자가 일치하고 중복된 X룸이 없으면 예외 없이 통과한다") {
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

        test("이미 X룸이 존재하는 TargetInfo이면 DuplicateException이 발생한다") {
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
})
