package com.konkuk.ma.domain.point.domain

import com.konkuk.ma.domain.point.domain.port.DiscountPolicyQueryRepository
import com.konkuk.ma.domain.point.domain.port.PointProductCacheRepository
import com.konkuk.ma.domain.point.domain.port.PointProductQueryRepository
import com.konkuk.ma.domain.point.fixture.DiscountPolicyFixture
import com.konkuk.ma.domain.point.fixture.PointProductFixture
import com.konkuk.ma.exception.EntityNotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class PointProductWithDiscountFinderTest : FunSpec({

    val pointProductQueryRepository = mockk<PointProductQueryRepository>()
    val discountPolicyQueryRepository = mockk<DiscountPolicyQueryRepository>()
    val pointProductCacheRepository = mockk<PointProductCacheRepository>()
    val finder = PointProductWithDiscountFinder(
        pointProductQueryRepository,
        discountPolicyQueryRepository,
        pointProductCacheRepository,
    )

    beforeEach {
        clearAllMocks()
    }

    context("findAll") {

        test("캐시에 데이터가 있으면 DB를 조회하지 않는다") {
            val cached = listOf(PointProductWithDiscount(PointProductFixture.create(), null))
            every { pointProductCacheRepository.findOrNull() } returns cached

            val result = finder.findAll()

            result shouldBe cached
            verify(exactly = 0) { pointProductQueryRepository.find() }
            verify(exactly = 0) { discountPolicyQueryRepository.find(any()) }
        }

        test("캐시 미스 시 DB에서 조회해 할인 정책과 결합한 뒤 캐시에 저장한다") {
            val policy = DiscountPolicyFixture.createAmount(discountPolicyId = 10L)
            val product = PointProductFixture.create(discountPolicyId = 10L)

            every { pointProductCacheRepository.findOrNull() } returns null
            every { pointProductQueryRepository.find() } returns listOf(product)
            every { discountPolicyQueryRepository.find(setOf(10L)) } returns listOf(policy)
            every { pointProductCacheRepository.save(any()) } returns Unit

            val result = finder.findAll()

            result shouldHaveSize 1
            result[0].discountPolicy shouldBe policy
            verify { pointProductCacheRepository.save(any()) }
        }

        test("캐시 조회 중 예외가 발생해도 DB 조회로 폴백한다") {
            val product = PointProductFixture.create(discountPolicyId = null)
            every { pointProductCacheRepository.findOrNull() } throws RuntimeException("Redis 연결 실패")
            every { pointProductQueryRepository.find() } returns listOf(product)
            every { discountPolicyQueryRepository.find(emptySet()) } returns emptyList()
            every { pointProductCacheRepository.save(any()) } returns Unit

            val result = finder.findAll()

            result shouldHaveSize 1
        }

        test("캐시 저장 중 예외가 발생해도 결과는 정상 반환된다") {
            val product = PointProductFixture.create(discountPolicyId = null)
            every { pointProductCacheRepository.findOrNull() } returns null
            every { pointProductQueryRepository.find() } returns listOf(product)
            every { discountPolicyQueryRepository.find(emptySet()) } returns emptyList()
            every { pointProductCacheRepository.save(any()) } throws RuntimeException("Redis 연결 실패")

            val result = finder.findAll()

            result shouldHaveSize 1
        }
    }

    context("findOne") {

        test("캐시에서 단건 조회가 성공하면 DB를 조회하지 않는다") {
            val cached = PointProductWithDiscount(
                PointProductFixture.create(pointProductId = 3L, discountPolicyId = null),
                null,
            )
            every { pointProductCacheRepository.findOneOrNull(3L) } returns cached

            val result = finder.findOne(3L)

            result shouldBe cached
            verify(exactly = 0) { pointProductQueryRepository.findOne(any()) }
        }

        test("캐시가 null을 반환하면 DB에서 상품 단건만 조회한다 (전체 목록 로드하지 않음)") {
            val product = PointProductFixture.create(pointProductId = 5L, discountPolicyId = null)
            every { pointProductCacheRepository.findOneOrNull(5L) } returns null
            every { pointProductQueryRepository.findOne(5L) } returns product

            val result = finder.findOne(5L)

            result.productId() shouldBe 5L
            verify(exactly = 0) { pointProductQueryRepository.find() }
            verify(exactly = 0) { pointProductCacheRepository.save(any()) }
        }

        test("캐시 미스에 할인 정책이 있는 상품이면 할인 정책도 단건 조회해 결합한다") {
            val policy = DiscountPolicyFixture.createAmount(discountPolicyId = 20L)
            val product = PointProductFixture.create(pointProductId = 6L, discountPolicyId = 20L)
            every { pointProductCacheRepository.findOneOrNull(6L) } returns null
            every { pointProductQueryRepository.findOne(6L) } returns product
            every { discountPolicyQueryRepository.findOneOrNull(20L) } returns policy

            val result = finder.findOne(6L)

            result.discountPolicy shouldBe policy
            verify(exactly = 0) { discountPolicyQueryRepository.find(any()) }
        }

        test("캐시와 DB 모두 해당 id가 없으면 DB가 던진 EntityNotFoundException이 전파된다") {
            every { pointProductCacheRepository.findOneOrNull(999L) } returns null
            every { pointProductQueryRepository.findOne(999L) } throws
                EntityNotFoundException(com.konkuk.ma.exception.EntityType.POINT_PRODUCT, "999")

            shouldThrow<EntityNotFoundException> {
                finder.findOne(999L)
            }
        }

        test("캐시 조회 중 예외가 발생하면 DB 단건 조회로 폴백한다") {
            val product = PointProductFixture.create(pointProductId = 7L, discountPolicyId = null)
            every { pointProductCacheRepository.findOneOrNull(7L) } throws RuntimeException("Redis 연결 실패")
            every { pointProductQueryRepository.findOne(7L) } returns product

            val result = finder.findOne(7L)

            result.productId() shouldBe 7L
        }
    }
})
