package com.konkuk.ma.domain.xroom.domain.block

import com.konkuk.ma.domain.xroom.fixture.XroomBlockFixture
import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NewShortTextBlockTest : FunSpec({

    context("init - 정상 생성") {

        test("PLAIN_CARD 아이템과 maxTextLength(200) 이하 텍스트면 정상 생성된다") {
            val content = "안녕하세요"
            val block = XroomBlockFixture.newShortTextBlock(content = content)

            block.type shouldBe XroomBlockType.SHORT_TEXT
            block.content shouldBe content
        }

        test("정확히 maxTextLength 길이 텍스트도 정상 생성된다") {
            val maxContent = "a".repeat(200)
            val block = XroomBlockFixture.newShortTextBlock(content = maxContent)

            block.content.length shouldBe 200
        }
    }

    context("init - 호환성 검증") {

        test("SHORT_TEXT 타입과 호환되지 않는 아이템이면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                XroomBlockFixture.newShortTextBlock(item = XroomBlockItem.POLAROID_FRAME)
            }
        }
    }

    context("init - 텍스트 길이 검증") {

        test("maxTextLength를 초과하는 텍스트면 InvalidValueException이 발생한다") {
            val overLengthContent = "a".repeat(201)

            shouldThrow<InvalidValueException> {
                XroomBlockFixture.newShortTextBlock(content = overLengthContent)
            }
        }
    }
})
