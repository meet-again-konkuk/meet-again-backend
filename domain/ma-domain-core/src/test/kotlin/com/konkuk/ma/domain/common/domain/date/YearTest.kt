package com.konkuk.ma.domain.common.domain.date

import com.konkuk.ma.domain.common.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class YearTest : FunSpec({

    context("Year 객체 생성 테스트") {

        test("현재 연도로 객체 생성 성공") {
            val currentYear = LocalDate.now().year
            val year = Year(currentYear)
            year.value shouldBe currentYear
        }

        test("1900으로 객체 생성 성공") {
            val year = Year(1900)
            year.value shouldBe 1900
        }

        test("1899로 객체 생성 실패") {
            shouldThrow<InvalidValueException> {
                Year(1899)
            }
        }

        test("현재 연도 + 1로 객체 생성 실패") {
            shouldThrow<InvalidValueException> {
                Year(LocalDate.now().year + 1)
            }
        }

        test("0으로 객체 생성 실패") {
            shouldThrow<InvalidValueException> {
                Year(0)
            }
        }

        test("음수로 객체 생성 실패") {
            shouldThrow<InvalidValueException> {
                Year(-1)
            }
        }
    }
})
