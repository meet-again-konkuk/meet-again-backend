package com.konkuk.ma.domain.xroom.domain.block

import com.konkuk.ma.domain.xroom.fixture.XroomBlockFixture
import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NewPhotoBlockTest : FunSpec({

    context("init - 정상 생성") {

        test("PHOTO 타입과 호환되는 아이템이면 정상 생성된다") {
            val block = XroomBlockFixture.newPhotoBlock(item = XroomBlockItem.POLAROID_FRAME)

            block.type shouldBe XroomBlockType.PHOTO
            block.item shouldBe XroomBlockItem.POLAROID_FRAME
        }

        test("photoDate가 null이어도 정상 생성된다") {
            val block = XroomBlockFixture.newPhotoBlock(photoDate = null)

            block.photoDate shouldBe null
        }
    }

    context("init - 호환성 검증") {

        test("PHOTO 타입과 호환되지 않는 아이템이면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                XroomBlockFixture.newPhotoBlock(item = XroomBlockItem.PLAIN_CARD)
            }
        }

        test("LONG_TEXT 전용 아이템(LETTER_PAPER)으로 PHOTO 블록을 만들면 예외가 발생한다") {
            shouldThrow<InvalidValueException> {
                XroomBlockFixture.newPhotoBlock(item = XroomBlockItem.LETTER_PAPER)
            }
        }
    }
})
