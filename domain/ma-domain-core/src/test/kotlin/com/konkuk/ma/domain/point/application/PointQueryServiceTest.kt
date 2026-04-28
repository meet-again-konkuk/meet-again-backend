package com.konkuk.ma.domain.point.application

import com.konkuk.ma.domain.point.domain.PointProductWithDiscount
import com.konkuk.ma.domain.point.domain.PointProductWithDiscountFinder
import com.konkuk.ma.domain.point.fixture.PointProductFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class PointQueryServiceTest : FunSpec({

    val productFinder = mockk<PointProductWithDiscountFinder>()
    val pointQueryService = PointQueryService(productFinder)

    context("findProducts") {

        test("Finder가 반환한 상품 목록을 그대로 반환한다") {
            val products = listOf(PointProductWithDiscount(PointProductFixture.create(), null))
            every { productFinder.findAll() } returns products

            val result = pointQueryService.findProducts()

            result shouldBe products
        }

        test("Finder가 빈 목록을 반환하면 빈 목록을 반환한다") {
            every { productFinder.findAll() } returns emptyList()

            val result = pointQueryService.findProducts()

            result shouldBe emptyList()
        }
    }
})
