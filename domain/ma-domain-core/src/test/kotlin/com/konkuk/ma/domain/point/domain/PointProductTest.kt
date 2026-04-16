package com.konkuk.ma.domain.point.domain

import com.konkuk.ma.domain.point.fixture.DiscountPolicyFixture
import com.konkuk.ma.domain.point.fixture.PointProductFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class PointProductTest : FunSpec({

    val now = LocalDate.of(2026, 4, 17)

    context("discountedPrice") {

        test("할인 정책이 없으면 원래 가격을 반환한다") {
            val product = PointProductFixture.create(price = 1000)

            product.discountedPrice(now) shouldBe 1000
        }

        test("활성 할인 정책이 있으면 할인된 가격을 반환한다") {
            val policy = DiscountPolicyFixture.createAmount(
                startDate = now.minusDays(1),
                endDate = now.plusDays(1),
                discountAmount = 300,
            )
            val product = PointProductFixture.create(price = 1000, discountPolicy = policy)

            product.discountedPrice(now) shouldBe 700
        }

        test("비활성 할인 정책이면 원래 가격을 반환한다") {
            val policy = DiscountPolicyFixture.createAmount(
                startDate = now.plusDays(1),
                endDate = now.plusDays(10),
                discountAmount = 300,
            )
            val product = PointProductFixture.create(price = 1000, discountPolicy = policy)

            product.discountedPrice(now) shouldBe 1000
        }

        test("비율 할인이 적용된 가격을 반환한다") {
            val policy = DiscountPolicyFixture.createPercent(
                startDate = now.minusDays(1),
                endDate = now.plusDays(1),
                discountPercent = 20,
            )
            val product = PointProductFixture.create(price = 1000, discountPolicy = policy)

            product.discountedPrice(now) shouldBe 800
        }
    }
})
