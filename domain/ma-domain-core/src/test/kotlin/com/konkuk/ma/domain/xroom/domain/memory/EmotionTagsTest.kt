package com.konkuk.ma.domain.xroom.domain.memory

import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

class EmotionTagsTest : FunSpec({

    context("of") {

        test("유효한 태그 목록으로 생성하면 data에 그대로 보존된다") {
            val tags = listOf("설렘", "행복")

            val emotionTags = EmotionTags.of(tags)

            emotionTags.data shouldBe tags
        }

        test("빈 목록으로 생성하면 data는 빈 리스트다") {
            val emotionTags = EmotionTags.of(emptyList())

            emotionTags.data.shouldBeEmpty()
        }

        test("공백 태그가 포함되면 예외가 발생한다") {
            shouldThrow<InvalidValueException> {
                EmotionTags.of(listOf("설렘", "  "))
            }
        }

        test("50자를 초과하는 태그가 포함되면 예외가 발생한다") {
            val tooLongTag = "가".repeat(51)

            shouldThrow<InvalidValueException> {
                EmotionTags.of(listOf(tooLongTag))
            }
        }

        test("50자 경계값 태그는 생성된다") {
            val maxLengthTag = "가".repeat(50)

            EmotionTags.of(listOf(maxLengthTag)).data shouldBe listOf(maxLengthTag)
        }
    }
})
