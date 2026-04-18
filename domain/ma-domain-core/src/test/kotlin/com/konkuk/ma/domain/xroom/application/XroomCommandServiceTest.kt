package com.konkuk.ma.domain.xroom.application

import com.konkuk.ma.domain.xroom.domain.NewXroom
import com.konkuk.ma.domain.xroom.domain.XroomValidator
import com.konkuk.ma.domain.xroom.domain.port.XroomCommandRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify

class XroomCommandServiceTest : FunSpec({

    val xroomCommandRepository = mockk<XroomCommandRepository>()
    val xroomValidator = mockk<XroomValidator>()
    val xroomCommandService = XroomCommandService(xroomCommandRepository, xroomValidator)

    context("create") {

        test("검증을 통과하면 X룸을 저장하고 ID를 반환한다") {
            val targetInfoId = 1L
            val email = "holeman@naver.com"
            every { xroomValidator.validate(any()) } just runs
            every { xroomCommandRepository.save(any()) } returns 1L

            val result = xroomCommandService.create(targetInfoId, email)

            result shouldBe 1L
            verify { xroomValidator.validate(any<NewXroom>()) }
            verify { xroomCommandRepository.save(any()) }
        }
    }
})
