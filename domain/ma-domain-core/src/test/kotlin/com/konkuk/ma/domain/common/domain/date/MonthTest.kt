package com.konkuk.ma.domain.common.domain.date

import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MonthTest : FunSpec({

    context("Month 객체 생성 테스트") {

        test("1월로 객체 생성 성공") {
            val month = Month(1)
            month.value shouldBe 1
        }

        test("12월로 객체 생성 성공") {
            val month = Month(12)
            month.value shouldBe 12
        }

        test("6월로 객체 생성 성공") {
            val month = Month(6)
            month.value shouldBe 6
        }

        test("0으로 객체 생성 실패") {
            shouldThrow<InvalidValueException> {
                Month(0)
            }
        }

        test("13으로 객체 생성 실패") {
            shouldThrow<InvalidValueException> {
                Month(13)
            }
        }

        test("음수로 객체 생성 실패") {
            shouldThrow<InvalidValueException> {
                Month(-1)
            }
        }
    }
})
