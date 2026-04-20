package com.konkuk.ma.domain.xroom.domain.block

import com.konkuk.ma.domain.xroom.fixture.XroomBlockFixture
import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NewVideoBlockTest : FunSpec({

    context("init - 정상 생성") {

        test("VIDEO 타입과 호환되는 아이템이면 정상 생성된다") {
            val block = XroomBlockFixture.newVideoBlock(item = XroomBlockItem.VIDEO_FRAME)

            block.type shouldBe XroomBlockType.VIDEO
        }

        test("description이 null이어도 정상 생성된다") {
            val block = XroomBlockFixture.newVideoBlock(description = null)

            block.description shouldBe null
        }
    }

    context("init - 호환성 검증") {

        test("VIDEO 타입과 호환되지 않는 아이템이면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                XroomBlockFixture.newVideoBlock(item = XroomBlockItem.POLAROID_FRAME)
            }
        }
    }
})
