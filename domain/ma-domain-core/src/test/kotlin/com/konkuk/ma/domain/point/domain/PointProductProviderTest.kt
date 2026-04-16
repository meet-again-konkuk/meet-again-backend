package com.konkuk.ma.domain.point.domain

import com.konkuk.ma.domain.point.domain.port.PointProductCacheRepository
import com.konkuk.ma.domain.point.fixture.PointProductFixture
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class PointProductProviderTest : FunSpec({

    val pointProductCacheRepository = mockk<PointProductCacheRepository>()
    val pointProductProvider = PointProductProvider(pointProductCacheRepository)

    context("findFromCache") {

        test("캐시에 데이터가 있으면 반환한다") {
            val products = listOf(PointProductFixture.create())
            every { pointProductCacheRepository.findOrNull() } returns products

            val result = pointProductProvider.findFromCache()

            result shouldBe products
        }

        test("캐시에 데이터가 없으면 null을 반환한다") {
            every { pointProductCacheRepository.findOrNull() } returns null

            val result = pointProductProvider.findFromCache()

            result shouldBe null
        }

        test("캐시 조회 중 예외가 발생하면 null을 반환한다") {
            every { pointProductCacheRepository.findOrNull() } throws RuntimeException("Redis 연결 실패")

            val result = pointProductProvider.findFromCache()

            result shouldBe null
        }
    }

    context("saveToCache") {

        test("캐시 저장 중 예외가 발생해도 예외를 전파하지 않는다") {
            val products = listOf(PointProductFixture.create())
            every { pointProductCacheRepository.save(products) } throws RuntimeException("Redis 연결 실패")

            pointProductProvider.saveToCache(products)

            verify { pointProductCacheRepository.save(products) }
        }
    }
})
