package com.konkuk.ma.domain.common.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

class MoneyTest : FunSpec({

    context("plus / minus") {
        test("두 금액을 더한다") {
            Money.wons(1000) + Money.wons(500) shouldBe Money.wons(1500)
        }
        test("두 금액을 뺀다") {
            Money.wons(1000) - Money.wons(300) shouldBe Money.wons(700)
        }
    }

    context("times") {
        test("비율(numerator/denominator)로 곱한다") {
            Money.wons(1000).times(80, 100) shouldBe Money.wons(800)
        }
        test("나눗셈 후 소수점은 버림") {
            Money.wons(1000).times(1, 3) shouldBe Money.wons(333)
        }
    }

    context("비교 연산") {
        test("isLessThan") {
            Money.wons(500).isLessThan(Money.wons(1000)).shouldBeTrue()
            Money.wons(1000).isLessThan(Money.wons(500)).shouldBeFalse()
        }
        test("isGreaterThanOrEqualTo") {
            Money.wons(1000).isGreaterThanOrEqualTo(Money.wons(1000)).shouldBeTrue()
            Money.wons(1000).isGreaterThanOrEqualTo(Money.wons(500)).shouldBeTrue()
            Money.wons(500).isGreaterThanOrEqualTo(Money.wons(1000)).shouldBeFalse()
        }
        test("isZero") {
            Money.ZERO.isZero().shouldBeTrue()
            Money.wons(0).isZero().shouldBeTrue()
            Money.wons(1).isZero().shouldBeFalse()
        }
    }

    context("equals") {
        test("같은 금액이면 equal (scale 무관)") {
            Money.wons(1000) shouldBe Money.wons(1000)
        }
    }

    context("percentageOf") {
        test("base의 몇 %인지 정수로 반환한다") {
            Money.wons(300).percentageOf(Money.wons(1000)) shouldBe 30
        }
        test("소수점 이하는 버림") {
            Money.wons(333).percentageOf(Money.wons(1000)) shouldBe 33
        }
        test("base가 0이면 0") {
            Money.wons(100).percentageOf(Money.ZERO) shouldBe 0
        }
        test("자신이 base와 같으면 100") {
            Money.wons(500).percentageOf(Money.wons(500)) shouldBe 100
        }
    }
})
