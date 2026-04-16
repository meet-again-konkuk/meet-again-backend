package com.konkuk.ma.domain.xroom.application

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.matching.domain.TargetInfo
import com.konkuk.ma.domain.matching.domain.port.TargetInfoQueryRepository
import com.konkuk.ma.domain.matching.fixture.TargetInfoFixture
import com.konkuk.ma.domain.xroom.domain.port.XroomCommandRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomQueryRepository
import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.DuplicateException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class XroomCommandServiceTest : FunSpec({

    val targetInfoQueryRepository = mockk<TargetInfoQueryRepository>()
    val xroomCommandRepository = mockk<XroomCommandRepository>()
    val xroomQueryRepository = mockk<XroomQueryRepository>()
    val xroomCommandService = XroomCommandService(
        targetInfoQueryRepository, xroomCommandRepository, xroomQueryRepository
    )

    context("create") {

        test("X룸을 정상적으로 생성한다") {
            val targetInfo = TargetInfoFixture.create()
            val email = targetInfo.registerEmail.value
            every { targetInfoQueryRepository.findOne(targetInfo.targetInfoId) } returns targetInfo
            every { xroomQueryRepository.existsByTargetInfoId(targetInfo.targetInfoId) } returns false
            every { xroomCommandRepository.save(any()) } returns 1L

            val result = xroomCommandService.create(targetInfo.targetInfoId, email)

            result shouldBe 1L
            verify { xroomCommandRepository.save(any()) }
        }

        test("본인 소유가 아닌 TargetInfo로 생성하면 예외가 발생한다") {
            val targetInfo = TargetInfoFixture.create()
            val otherEmail = "other@example.com"
            every { targetInfoQueryRepository.findOne(targetInfo.targetInfoId) } returns targetInfo

            shouldThrow<AccessDeniedException> {
                xroomCommandService.create(targetInfo.targetInfoId, otherEmail)
            }
        }

        test("이미 X룸이 존재하는 TargetInfo로 생성하면 예외가 발생한다") {
            val targetInfo = TargetInfoFixture.create()
            val email = targetInfo.registerEmail.value
            every { targetInfoQueryRepository.findOne(targetInfo.targetInfoId) } returns targetInfo
            every { xroomQueryRepository.existsByTargetInfoId(targetInfo.targetInfoId) } returns true

            shouldThrow<DuplicateException> {
                xroomCommandService.create(targetInfo.targetInfoId, email)
            }
        }
    }
})
