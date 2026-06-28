package com.konkuk.ma.domain.xroom.domain.memory

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class EventDatePrecisionTest : FunSpec({

    context("from") {

        test("대문자 문자열로 정밀도를 생성한다") {
            EventDatePrecision.from("DAY") shouldBe EventDatePrecision.DAY
        }

        test("소문자 문자열도 대소문자 무관하게 정밀도를 생성한다") {
            EventDatePrecision.from("year") shouldBe EventDatePrecision.YEAR
        }

        test("혼합 대소문자도 정밀도를 생성한다") {
            EventDatePrecision.from("Month") shouldBe EventDatePrecision.MONTH
        }

        test("정의되지 않은 값이면 예외가 발생한다") {
            shouldThrow<IllegalArgumentException> {
                EventDatePrecision.from("WEEK")
            }
        }
    }
})
