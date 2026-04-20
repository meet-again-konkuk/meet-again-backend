package com.konkuk.ma.domain.xroom.domain.block

import com.konkuk.ma.domain.xroom.domain.port.XroomBlockPhotoQueryRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomBlockVideoQueryRepository
import com.konkuk.ma.domain.xroom.domain.port.XroomQueryRepository
import com.konkuk.ma.domain.xroom.fixture.XroomBlockFixture
import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.mockk.mockk

class XroomBlockValidatorTest : FunSpec({

    val xroomBlockValidator = XroomBlockValidator(
        xroomQueryRepository = mockk<XroomQueryRepository>(),
        xroomBlockPhotoQueryRepository = mockk<XroomBlockPhotoQueryRepository>(),
        xroomBlockVideoQueryRepository = mockk<XroomBlockVideoQueryRepository>(),
    )

    context("validate - 좌표 범위 검증") {

        test("positionX가 1, positionY가 1이면 예외 없이 통과한다") {
            val block = XroomBlockFixture.newPhotoBlock(positionX = 1, positionY = 1)

            xroomBlockValidator.validate(block)
        }

        test("positionX가 255, positionY가 255이면 예외 없이 통과한다") {
            val block = XroomBlockFixture.newPhotoBlock(positionX = 255, positionY = 255)

            xroomBlockValidator.validate(block)
        }

        test("positionX가 0이면 InvalidValueException이 발생한다") {
            val block = XroomBlockFixture.newPhotoBlock(positionX = 0)

            shouldThrow<InvalidValueException> {
                xroomBlockValidator.validate(block)
            }
        }

        test("positionX가 256이면 InvalidValueException이 발생한다") {
            val block = XroomBlockFixture.newPhotoBlock(positionX = 256)

            shouldThrow<InvalidValueException> {
                xroomBlockValidator.validate(block)
            }
        }

        test("positionY가 0이면 InvalidValueException이 발생한다") {
            val block = XroomBlockFixture.newPhotoBlock(positionY = 0)

            shouldThrow<InvalidValueException> {
                xroomBlockValidator.validate(block)
            }
        }

        test("positionY가 256이면 InvalidValueException이 발생한다") {
            val block = XroomBlockFixture.newPhotoBlock(positionY = 256)

            shouldThrow<InvalidValueException> {
                xroomBlockValidator.validate(block)
            }
        }
    }
})
