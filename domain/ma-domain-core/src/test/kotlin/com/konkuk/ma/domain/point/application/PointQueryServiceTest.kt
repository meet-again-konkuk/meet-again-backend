package com.konkuk.ma.domain.point.application

import com.konkuk.ma.domain.point.domain.PointProduct
import com.konkuk.ma.domain.point.domain.PointProductProvider
import com.konkuk.ma.domain.point.domain.port.PointProductQueryRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class PointQueryServiceTest : FunSpec({

    val pointProductQueryRepository = mockk<PointProductQueryRepository>()
    val pointProductProvider = mockk<PointProductProvider>()
    val pointQueryService = PointQueryService(pointProductQueryRepository, pointProductProvider)

    context("findProducts") {

        test("캐시에 데이터가 있으면 DB를 조회하지 않는다") {
            val cached = listOf(createPointProduct())
            every { pointProductProvider.findFromCache() } returns cached

            val result = pointQueryService.findProducts()

            result shouldBe cached
            verify(exactly = 0) { pointProductQueryRepository.find() }
        }

        test("캐시에 데이터가 없으면 DB에서 조회하고 캐시에 저장한다") {
            val products = listOf(createPointProduct())
            every { pointProductProvider.findFromCache() } returns null
            every { pointProductQueryRepository.find() } returns products
            every { pointProductProvider.saveToCache(products) } returns Unit

            val result = pointQueryService.findProducts()

            result shouldBe products
            verify { pointProductQueryRepository.find() }
            verify { pointProductProvider.saveToCache(products) }
        }

        test("캐시와 DB 모두 데이터가 없으면 빈 리스트를 반환한다") {
            every { pointProductProvider.findFromCache() } returns null
            every { pointProductQueryRepository.find() } returns emptyList()
            every { pointProductProvider.saveToCache(emptyList()) } returns Unit

            val result = pointQueryService.findProducts()

            result shouldHaveSize 0
        }
    }
})

private fun createPointProduct(
    pointProductId: Long = 1L,
    name: String = "인연 10개",
    quantity: Int = 10,
    price: Int = 1000,
    displayOrder: Int = 1,
): PointProduct {
    return PointProduct(
        pointProductId = pointProductId,
        name = name,
        quantity = quantity,
        price = price,
        displayOrder = displayOrder,
    )
}
