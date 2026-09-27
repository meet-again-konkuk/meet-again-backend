package com.konkuk.ma.domain.xroom.domain.memory

import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MemoryContentTest : FunSpec({

    context("of") {

        test("텍스트만 입력하면 텍스트가 채워지고 편지는 null이다") {
            val content = MemoryContent.of(text = "그날의 기억", letter = null)

            content.text shouldBe "그날의 기억"
            content.letter shouldBe null
        }

        test("편지만 입력하면 편지가 채워지고 텍스트는 null이다") {
            val content = MemoryContent.of(text = null, letter = "보고 싶었어")

            content.text shouldBe null
            content.letter shouldBe "보고 싶었어"
        }

        test("텍스트와 편지가 모두 null이어도 생성된다") {
            val content = MemoryContent.of(text = null, letter = null)

            content.text shouldBe null
            content.letter shouldBe null
        }

        test("텍스트와 편지를 모두 입력하면 예외가 발생한다") {
            shouldThrow<InvalidValueException> {
                MemoryContent.of(text = "그날의 기억", letter = "보고 싶었어")
            }
        }

        test("공백 텍스트는 null로 정규화된다") {
            val content = MemoryContent.of(text = "   ", letter = null)

            content.text shouldBe null
        }

        test("공백 편지는 null로 정규화된다") {
            val content = MemoryContent.of(text = null, letter = "   ")

            content.letter shouldBe null
        }

        test("공백 텍스트와 정상 편지를 함께 입력하면 상호배타 위반이 아니다") {
            val content = MemoryContent.of(text = "   ", letter = "보고 싶었어")

            content.text shouldBe null
            content.letter shouldBe "보고 싶었어"
        }
    }
})
