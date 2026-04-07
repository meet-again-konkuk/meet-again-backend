package com.konkuk.ma.domain.common.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class TimeAgoCalculatorTest : FunSpec({

    context("calculate") {

        val now = LocalDateTime.of(2026, 4, 6, 12, 0, 0)

        test("1분 미만이면 '방금 전'을 반환한다") {
            TimeAgoCalculator.calculate(now.minusSeconds(30), now) shouldBe "방금 전"
        }

        test("정확히 0초 차이면 '방금 전'을 반환한다") {
            TimeAgoCalculator.calculate(now, now) shouldBe "방금 전"
        }

        test("1분 이상 1시간 미만이면 '분 전'을 반환한다") {
            TimeAgoCalculator.calculate(now.minusMinutes(30), now) shouldBe "30분 전"
        }

        test("정확히 1분이면 '1분 전'을 반환한다") {
            TimeAgoCalculator.calculate(now.minusMinutes(1), now) shouldBe "1분 전"
        }

        test("1시간 이상 1일 미만이면 '시간 전'을 반환한다") {
            TimeAgoCalculator.calculate(now.minusHours(5), now) shouldBe "5시간 전"
        }

        test("정확히 1시간이면 '1시간 전'을 반환한다") {
            TimeAgoCalculator.calculate(now.minusHours(1), now) shouldBe "1시간 전"
        }

        test("1일 이상 30일 미만이면 '일 전'을 반환한다") {
            TimeAgoCalculator.calculate(now.minusDays(7), now) shouldBe "7일 전"
        }

        test("정확히 1일이면 '1일 전'을 반환한다") {
            TimeAgoCalculator.calculate(now.minusDays(1), now) shouldBe "1일 전"
        }

        test("30일 이상 365일 미만이면 '개월 전'을 반환한다") {
            TimeAgoCalculator.calculate(now.minusDays(90), now) shouldBe "3개월 전"
        }

        test("정확히 30일이면 '1개월 전'을 반환한다") {
            TimeAgoCalculator.calculate(now.minusDays(30), now) shouldBe "1개월 전"
        }

        test("365일 이상이면 '년 전'을 반환한다") {
            TimeAgoCalculator.calculate(now.minusDays(730), now) shouldBe "2년 전"
        }

        test("정확히 365일이면 '1년 전'을 반환한다") {
            TimeAgoCalculator.calculate(now.minusDays(365), now) shouldBe "1년 전"
        }
    }
})
