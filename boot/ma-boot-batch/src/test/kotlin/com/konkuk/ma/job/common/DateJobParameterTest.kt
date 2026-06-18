package com.konkuk.ma.job.common

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.format.DateTimeParseException

class DateJobParameterTest : FunSpec({

    context("inputDate 파싱") {

        test("유효한 날짜 문자열이 주어지면 해당 날짜로 파싱된다") {
            DateJobParameter("2025-03-15").inputDate shouldBe LocalDate.of(2025, 3, 15)
        }

        test("null이 주어지면 오늘 날짜가 기본값으로 설정된다") {
            DateJobParameter(null).inputDate shouldBe LocalDate.now()
        }

        test("다른 유효한 날짜 문자열도 파싱된다") {
            DateJobParameter("2024-12-31").inputDate shouldBe LocalDate.of(2024, 12, 31)
        }

        test("빈 문자열이 주어지면 DateTimeParseException이 발생한다") {
            shouldThrow<DateTimeParseException> { DateJobParameter("") }
        }

        test("잘못된 형식의 문자열이 주어지면 DateTimeParseException이 발생한다") {
            shouldThrow<DateTimeParseException> { DateJobParameter("2025/03/15") }
        }

        test("존재하지 않는 날짜가 주어지면 DateTimeParseException이 발생한다") {
            shouldThrow<DateTimeParseException> { DateJobParameter("2025-02-30") }
        }
    }
})
