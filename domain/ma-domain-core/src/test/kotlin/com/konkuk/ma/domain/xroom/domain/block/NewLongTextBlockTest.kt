package com.konkuk.ma.domain.xroom.domain.block

import com.konkuk.ma.domain.xroom.fixture.XroomBlockFixture
import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NewLongTextBlockTest : FunSpec({

    context("init - 정상 생성") {

        test("LETTER_PAPER 아이템과 maxTextLength(2000) 이하 텍스트면 정상 생성된다") {
            val content = "오랜만이야"
            val block = XroomBlockFixture.newLongTextBlock(content = content)

            block.type shouldBe XroomBlockType.LONG_TEXT
            block.content shouldBe content
        }

        test("LONG_LETTER 아이템은 10000자까지 허용한다") {
            val content = "a".repeat(10000)
            val block = XroomBlockFixture.newLongTextBlock(
                item = XroomBlockItem.LONG_LETTER,
                content = content,
            )

            block.content.length shouldBe 10000
        }
    }

    context("init - 호환성 검증") {

        test("LONG_TEXT 타입과 호환되지 않는 아이템이면 InvalidValueException이 발생한다") {
            shouldThrow<InvalidValueException> {
                XroomBlockFixture.newLongTextBlock(item = XroomBlockItem.PLAIN_CARD)
            }
        }
    }

    context("init - 텍스트 길이 검증") {

        test("maxTextLength를 초과하는 텍스트면 InvalidValueException이 발생한다") {
            val overLengthContent = "a".repeat(2001)

            shouldThrow<InvalidValueException> {
                XroomBlockFixture.newLongTextBlock(content = overLengthContent)
            }
        }
    }
})
