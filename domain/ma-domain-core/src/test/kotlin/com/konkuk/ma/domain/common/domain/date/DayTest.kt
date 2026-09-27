package com.konkuk.ma.domain.common.domain.date

import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DayTest : FunSpec({

    context("Day 객체 생성 테스트") {

        test("1일로 객체 생성 성공") {
            val day = Day(1)
            day.value shouldBe 1
        }

        test("31일로 객체 생성 성공") {
            val day = Day(31)
            day.value shouldBe 31
        }

        test("15일로 객체 생성 성공") {
            val day = Day(15)
            day.value shouldBe 15
        }

        test("0으로 객체 생성 실패") {
            shouldThrow<InvalidValueException> {
                Day(0)
            }
        }

        test("32로 객체 생성 실패") {
            shouldThrow<InvalidValueException> {
                Day(32)
            }
        }

        test("음수로 객체 생성 실패") {
            shouldThrow<InvalidValueException> {
                Day(-1)
            }
        }
    }
})
