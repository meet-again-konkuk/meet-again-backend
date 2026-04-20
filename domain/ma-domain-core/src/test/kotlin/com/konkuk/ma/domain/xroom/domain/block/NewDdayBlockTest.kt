package com.konkuk.ma.domain.xroom.domain.block

import com.konkuk.ma.domain.xroom.fixture.XroomBlockFixture
import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class NewDdayBlockTest : FunSpec({

    context("init - 정상 생성") {

        test("anniversaryDate와 label이 채워져 있으면 정상 생성된다") {
            val block = XroomBlockFixture.newDdayBlock(
                anniversaryDate = LocalDate.of(2024, 1, 1),
                label = "사귄지",
            )

            block.type shouldBe XroomBlockType.DDAY
            block.label shouldBe "사귄지"
        }
    }

    context("init - 호환성 검증") {

        test("DDAY 타입과 호환되지 않는 아이템이면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                XroomBlockFixture.newDdayBlock(item = XroomBlockItem.POLAROID_FRAME)
            }
        }
    }

    context("init - label 검증") {

        test("label이 빈 문자열이면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                XroomBlockFixture.newDdayBlock(label = "")
            }
        }

        test("label이 공백만 있으면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                XroomBlockFixture.newDdayBlock(label = "   ")
            }
        }
    }
})
