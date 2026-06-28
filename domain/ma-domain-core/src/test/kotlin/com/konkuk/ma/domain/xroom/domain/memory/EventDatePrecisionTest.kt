package com.konkuk.ma.domain.xroom.domain.memory

import com.konkuk.ma.exception.InvalidValueException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

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
            shouldThrow<InvalidValueException> {
                EventDatePrecision.from("WEEK")
            }
        }
    }

    context("format") {

        test("YEAR는 정규화된 날짜를 'yyyy'로 렌더링한다") {
            EventDatePrecision.YEAR.format(LocalDate.of(2019, 1, 1)) shouldBe "2019"
        }

        test("MONTH는 정규화된 날짜를 'yyyy-MM'으로 렌더링한다") {
            EventDatePrecision.MONTH.format(LocalDate.of(2019, 5, 1)) shouldBe "2019-05"
        }

        test("DAY는 정규화된 날짜를 'yyyy-MM-dd'로 렌더링한다") {
            EventDatePrecision.DAY.format(LocalDate.of(2019, 5, 10)) shouldBe "2019-05-10"
        }
    }

    context("parse") {

        test("YEAR는 'yyyy'를 해당 연도 1월 1일로 정규화한다") {
            EventDatePrecision.YEAR.parse("2019") shouldBe LocalDate.of(2019, 1, 1)
        }

        test("MONTH는 'yyyy-MM'을 해당 월 1일로 정규화한다") {
            EventDatePrecision.MONTH.parse("2019-05") shouldBe LocalDate.of(2019, 5, 1)
        }

        test("DAY는 'yyyy-MM-dd'를 실제 날짜로 정규화한다") {
            EventDatePrecision.DAY.parse("2019-05-10") shouldBe LocalDate.of(2019, 5, 10)
        }

        test("정밀도와 세그먼트 수가 맞지 않으면 예외가 발생한다") {
            shouldThrow<InvalidValueException> {
                EventDatePrecision.YEAR.parse("2019-05")
            }
        }

        test("존재하지 않는 날짜면 예외가 발생한다") {
            shouldThrow<InvalidValueException> {
                EventDatePrecision.DAY.parse("2019-02-30")
            }
        }
    }
})
