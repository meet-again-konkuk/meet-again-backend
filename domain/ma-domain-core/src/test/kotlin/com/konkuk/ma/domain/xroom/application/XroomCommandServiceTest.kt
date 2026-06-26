package com.konkuk.ma.domain.xroom.application

import com.konkuk.ma.domain.xroom.domain.NewXroom
import com.konkuk.ma.domain.xroom.domain.XroomValidator
import com.konkuk.ma.domain.xroom.domain.port.XroomCommandRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomQueryRepository
import com.konkuk.ma.domain.xroom.fixture.XroomFixture
import com.konkuk.ma.exception.AccessDeniedException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify

class XroomCommandServiceTest : FunSpec({

    val xroomCommandRepository = mockk<XroomCommandRepository>()
    val xroomQueryRepository = mockk<XroomQueryRepository>()
    val xroomValidator = mockk<XroomValidator>()
    val xroomCommandService =
        XroomCommandService(xroomCommandRepository, xroomQueryRepository, xroomValidator)

    context("create") {

        test("검증을 통과하면 방을 저장하고 ID를 반환한다") {
            val targetInfoId = 1L
            val memberId = 1L
            every { xroomValidator.validate(any()) } just runs
            every { xroomCommandRepository.save(any()) } returns 1L

            val result = xroomCommandService.create(targetInfoId, memberId, null)

            result shouldBe 1L
            verify { xroomValidator.validate(any<NewXroom>()) }
            verify { xroomCommandRepository.save(any()) }
        }
    }

    context("updateFinalMessage") {

        test("작성자가 요청하면 마지막 메시지를 수정하고 ID를 반환한다") {
            val memberId = 1L
            val xroom = XroomFixture.create(id = 1L, ownerId = memberId)
            every { xroomQueryRepository.findOne(xroom.id) } returns xroom
            every { xroomCommandRepository.updateFinalMessage(any()) } just runs

            val result = xroomCommandService.updateFinalMessage(xroom.id, memberId, "고마웠어")

            result shouldBe xroom.id
            verify { xroomCommandRepository.updateFinalMessage(any()) }
        }

        test("작성자가 아니면 예외를 던진다") {
            val xroom = XroomFixture.create(id = 1L, ownerId = 99L)
            every { xroomQueryRepository.findOne(xroom.id) } returns xroom

            shouldThrow<AccessDeniedException> {
                xroomCommandService.updateFinalMessage(xroom.id, 1L, "고마웠어")
            }
        }
    }
})
