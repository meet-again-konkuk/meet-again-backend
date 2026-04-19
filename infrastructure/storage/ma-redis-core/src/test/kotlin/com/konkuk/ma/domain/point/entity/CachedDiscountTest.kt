package com.konkuk.ma.domain.point.entity

import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.domain.discount.AmountDiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.DiscountType
import com.konkuk.ma.domain.point.domain.discount.PercentDiscountPolicy
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.LocalDate

class CachedDiscountTest : FunSpec({

    context("CachedAmountDiscount.toDomain") {

        test("도메인 AmountDiscountPolicy로 변환한다") {
            val cached = CachedAmountDiscount(
                startDate = "2026-04-01",
                endDate = "2026-05-01",
                discountAmount = 200,
            )

            val domain = cached.toDomain()

            domain.shouldBeInstanceOf<AmountDiscountPolicy>()
            domain.startDate shouldBe LocalDate.of(2026, 4, 1)
            domain.endDate shouldBe LocalDate.of(2026, 5, 1)
            domain.discountAmount shouldBe Money.wons(200)
        }
    }

    context("CachedPercentDiscount.toDomain") {

        test("도메인 PercentDiscountPolicy로 변환한다") {
            val cached = CachedPercentDiscount(
                startDate = "2026-04-01",
                endDate = "2026-05-01",
                discountPercent = 25,
            )

            val domain = cached.toDomain()

            domain.shouldBeInstanceOf<PercentDiscountPolicy>()
            domain.discountPercent shouldBe 25
        }
    }

    context("CachedDiscount.fromPolicy") {

        test("AmountDiscountPolicy → CachedAmountDiscount") {
            val policy = AmountDiscountPolicy(
                discountPolicyId = 1L,
                startDate = LocalDate.of(2026, 4, 1),
                endDate = LocalDate.of(2026, 5, 1),
                discountAmount = Money.wons(500),
            )

            val cached = CachedDiscount.fromPolicy(policy)

            cached.shouldBeInstanceOf<CachedAmountDiscount>()
            cached.startDate shouldBe "2026-04-01"
            cached.endDate shouldBe "2026-05-01"
            cached.discountAmount shouldBe 500
        }

        test("PercentDiscountPolicy → CachedPercentDiscount") {
            val policy = PercentDiscountPolicy(
                discountPolicyId = 2L,
                startDate = LocalDate.of(2026, 4, 1),
                endDate = LocalDate.of(2026, 5, 1),
                discountPercent = 30,
            )

            val cached = CachedDiscount.fromPolicy(policy)

            cached.shouldBeInstanceOf<CachedPercentDiscount>()
            cached.discountPercent shouldBe 30
        }
    }

    context("type 프로퍼티") {

        test("각 서브타입이 자기 DiscountType을 노출한다") {
            CachedAmountDiscount().type shouldBe DiscountType.AMOUNT
            CachedPercentDiscount().type shouldBe DiscountType.PERCENT
        }
    }
})
