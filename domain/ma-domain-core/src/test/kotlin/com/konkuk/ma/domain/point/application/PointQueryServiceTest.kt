package com.konkuk.ma.domain.point.application

import com.konkuk.ma.domain.point.domain.PointProductProvider
import com.konkuk.ma.domain.point.domain.port.DiscountPolicyQueryRepository
import com.konkuk.ma.domain.point.domain.port.PointProductQueryRepository
import com.konkuk.ma.domain.point.fixture.DiscountPolicyFixture
import com.konkuk.ma.domain.point.fixture.PointProductFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class PointQueryServiceTest : FunSpec({

    val pointProductQueryRepository = mockk<PointProductQueryRepository>()
    val discountPolicyQueryRepository = mockk<DiscountPolicyQueryRepository>()
    val pointProductProvider = mockk<PointProductProvider>()
    val pointQueryService = PointQueryService(
        pointProductQueryRepository,
        discountPolicyQueryRepository,
        pointProductProvider,
    )

    context("findProducts") {

        test("캐시에 데이터가 있으면 DB를 조회하지 않는다") {
            val cached = listOf(PointProductFixture.create())
            every { pointProductProvider.findFromCache() } returns cached

            val result = pointQueryService.findProducts()

            result shouldBe cached
            verify(exactly = 0) { pointProductQueryRepository.find() }
            verify(exactly = 0) { discountPolicyQueryRepository.find(any()) }
        }

        test("캐시 미스 시 DB에서 조회하고 할인 정책과 결합한 뒤 캐시에 저장한다") {
            val policy = DiscountPolicyFixture.createAmount(discountPolicyId = 10L)
            val productWithPolicyRef = PointProductFixture.create(discountPolicyId = 10L)

            every { pointProductProvider.findFromCache() } returns null
            every { pointProductQueryRepository.find() } returns listOf(productWithPolicyRef)
            every { discountPolicyQueryRepository.find(setOf(10L)) } returns listOf(policy)
            every { pointProductProvider.saveToCache(any()) } returns Unit

            val result = pointQueryService.findProducts()

            result shouldHaveSize 1
            result[0].discountPolicy shouldBe policy
            verify { pointProductQueryRepository.find() }
            verify { discountPolicyQueryRepository.find(setOf(10L)) }
            verify { pointProductProvider.saveToCache(any()) }
        }

        test("할인 정책이 없는 상품은 discountPolicy가 null인 채로 반환된다") {
            val product = PointProductFixture.create(discountPolicyId = null)

            every { pointProductProvider.findFromCache() } returns null
            every { pointProductQueryRepository.find() } returns listOf(product)
            every { discountPolicyQueryRepository.find(emptySet()) } returns emptyList()
            every { pointProductProvider.saveToCache(any()) } returns Unit

            val result = pointQueryService.findProducts()

            result shouldHaveSize 1
            result[0].discountPolicy shouldBe null
        }

        test("캐시와 DB 모두 데이터가 없으면 빈 리스트를 반환한다") {
            every { pointProductProvider.findFromCache() } returns null
            every { pointProductQueryRepository.find() } returns emptyList()
            every { discountPolicyQueryRepository.find(emptySet()) } returns emptyList()
            every { pointProductProvider.saveToCache(emptyList()) } returns Unit

            val result = pointQueryService.findProducts()

            result shouldHaveSize 0
        }
    }
})
