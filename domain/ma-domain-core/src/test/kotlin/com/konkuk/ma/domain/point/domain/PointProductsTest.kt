package com.konkuk.ma.domain.point.domain

import com.konkuk.ma.domain.point.fixture.DiscountPolicyFixture
import com.konkuk.ma.domain.point.fixture.PointProductFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class PointProductsTest : FunSpec({

    context("extractDiscountPolicyIds") {

        test("중복 없이 존재하는 policy ID만 추출한다") {
            val products = PointProducts(
                listOf(
                    PointProductFixture.create(pointProductId = 1L, discountPolicyId = 10L),
                    PointProductFixture.create(pointProductId = 2L, discountPolicyId = null),
                    PointProductFixture.create(pointProductId = 3L, discountPolicyId = 10L),
                    PointProductFixture.create(pointProductId = 4L, discountPolicyId = 20L),
                )
            )

            products.extractDiscountPolicyIds() shouldContainExactlyInAnyOrder setOf(10L, 20L)
        }

        test("모두 null이면 빈 Set을 반환한다") {
            val products = PointProducts(listOf(PointProductFixture.create(discountPolicyId = null)))

            products.extractDiscountPolicyIds() shouldHaveSize 0
        }
    }

    context("combineWith") {

        test("각 상품에 해당 policy가 결합된다") {
            val policy10 = DiscountPolicyFixture.createAmount(discountPolicyId = 10L)
            val policy20 = DiscountPolicyFixture.createPercent(discountPolicyId = 20L)
            val products = PointProducts(
                listOf(
                    PointProductFixture.create(pointProductId = 1L, discountPolicyId = 10L),
                    PointProductFixture.create(pointProductId = 2L, discountPolicyId = 20L),
                    PointProductFixture.create(pointProductId = 3L, discountPolicyId = null),
                )
            )

            val combined = products.combineWith(listOf(policy10, policy20))

            combined.data[0].discountPolicy shouldBe policy10
            combined.data[1].discountPolicy shouldBe policy20
            combined.data[2].discountPolicy shouldBe null
        }

        test("매칭되는 policy가 없으면 discountPolicy는 null") {
            val products = PointProducts(
                listOf(PointProductFixture.create(discountPolicyId = 10L))
            )

            val combined = products.combineWith(emptyList())

            combined.data[0].discountPolicy shouldBe null
        }
    }
})
