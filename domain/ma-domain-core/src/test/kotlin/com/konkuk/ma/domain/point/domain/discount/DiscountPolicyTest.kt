package com.konkuk.ma.domain.point.domain.discount

import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.fixture.DiscountPolicyFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class DiscountPolicyTest : FunSpec({

    val now = LocalDate.of(2026, 4, 17)

    context("isActive") {

        test("현재 날짜가 기간 내이면 true를 반환한다") {
            val policy = DiscountPolicyFixture.createAmount(
                startDate = now.minusDays(1),
                endDate = now.plusDays(1),
            )

            policy.isActive(now).shouldBeTrue()
        }

        test("현재 날짜가 시작일과 같으면 true를 반환한다") {
            val policy = DiscountPolicyFixture.createAmount(
                startDate = now,
                endDate = now.plusDays(10),
            )

            policy.isActive(now).shouldBeTrue()
        }

        test("현재 날짜가 종료일과 같으면 true를 반환한다") {
            val policy = DiscountPolicyFixture.createAmount(
                startDate = now.minusDays(10),
                endDate = now,
            )

            policy.isActive(now).shouldBeTrue()
        }

        test("현재 날짜가 시작일 이전이면 false를 반환한다") {
            val policy = DiscountPolicyFixture.createAmount(
                startDate = now.plusDays(1),
                endDate = now.plusDays(10),
            )

            policy.isActive(now).shouldBeFalse()
        }

        test("현재 날짜가 종료일 이후이면 false를 반환한다") {
            val policy = DiscountPolicyFixture.createAmount(
                startDate = now.minusDays(10),
                endDate = now.minusDays(1),
            )

            policy.isActive(now).shouldBeFalse()
        }
    }

    context("AmountDiscountPolicy.calculateDiscountedPrice") {

        test("고정 금액을 할인한다") {
            val policy = DiscountPolicyFixture.createAmount(discountAmount = 500)

            policy.calculateDiscountedPrice(Money.wons(1000)) shouldBe Money.wons(500)
        }

        test("할인 금액이 가격보다 크면 0을 반환한다") {
            val policy = DiscountPolicyFixture.createAmount(discountAmount = 1500)

            policy.calculateDiscountedPrice(Money.wons(1000)) shouldBe Money.ZERO
        }
    }

    context("PercentDiscountPolicy.calculateDiscountedPrice") {

        test("비율로 할인한다") {
            val policy = DiscountPolicyFixture.createPercent(discountPercent = 10)

            policy.calculateDiscountedPrice(Money.wons(1000)) shouldBe Money.wons(900)
        }

        test("100% 할인이면 0을 반환한다") {
            val policy = DiscountPolicyFixture.createPercent(discountPercent = 100)

            policy.calculateDiscountedPrice(Money.wons(1000)) shouldBe Money.ZERO
        }

        test("0% 할인이면 원래 가격을 반환한다") {
            val policy = DiscountPolicyFixture.createPercent(discountPercent = 0)

            policy.calculateDiscountedPrice(Money.wons(1000)) shouldBe Money.wons(1000)
        }
    }
})
