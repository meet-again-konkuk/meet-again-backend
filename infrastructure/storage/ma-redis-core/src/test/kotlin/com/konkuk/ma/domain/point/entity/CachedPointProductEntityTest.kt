package com.konkuk.ma.domain.point.entity

import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.domain.PointProduct
import com.konkuk.ma.domain.point.domain.PointProductWithDiscount
import com.konkuk.ma.domain.point.domain.discount.AmountDiscountPolicy
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.LocalDate

class CachedPointProductEntityTest : FunSpec({

    context("from(product)") {

        test("할인 정책이 있으면 CachedDiscount까지 포함해서 생성한다") {
            val policy = AmountDiscountPolicy(
                discountPolicyId = 10L,
                startDate = LocalDate.of(2026, 4, 1),
                endDate = LocalDate.of(2026, 5, 1),
                discountAmount = Money.wons(200),
            )
            val product = PointProductWithDiscount(
                pointProduct = PointProduct(
                    pointProductId = 1L,
                    name = "인연 10개",
                    quantity = 10,
                    price = Money.wons(1000),
                    displayOrder = 1,
                    discountPolicyId = 10L,
                ),
                discountPolicy = policy,
            )

            val cached = CachedPointProductEntity.from(product)

            cached.pointProductId shouldBe 1L
            cached.name shouldBe "인연 10개"
            cached.price shouldBe 1000
            cached.discount.shouldBeInstanceOf<CachedAmountDiscount>()
        }

        test("할인 정책이 없으면 discount는 null") {
            val product = PointProductWithDiscount(
                pointProduct = PointProduct(
                    pointProductId = 2L,
                    name = "인연 30개",
                    quantity = 30,
                    price = Money.wons(2500),
                    displayOrder = 2,
                ),
                discountPolicy = null,
            )

            val cached = CachedPointProductEntity.from(product)

            cached.discount shouldBe null
        }
    }

    context("toDomain") {

        test("CachedDiscount가 있으면 도메인 PointProductWithDiscount로 변환하며 정책도 복원한다") {
            val cached = CachedPointProductEntity(
                pointProductId = 1L,
                name = "인연 10개",
                quantity = 10,
                price = 1000,
                displayOrder = 1,
                discount = CachedAmountDiscount(
                    startDate = "2026-04-01",
                    endDate = "2026-05-01",
                    discountAmount = 200,
                ),
            )

            val result = cached.toDomain()

            result.pointProduct.pointProductId shouldBe 1L
            result.pointProduct.price shouldBe Money.wons(1000)
            result.discountPolicy.shouldBeInstanceOf<AmountDiscountPolicy>()
        }

        test("CachedDiscount가 null이면 discountPolicy/discountPolicyId도 null") {
            val cached = CachedPointProductEntity(
                pointProductId = 2L,
                name = "인연 30개",
                quantity = 30,
                price = 2500,
                displayOrder = 2,
            )

            val result = cached.toDomain()

            result.pointProduct.discountPolicyId shouldBe null
            result.discountPolicy shouldBe null
        }
    }
})
