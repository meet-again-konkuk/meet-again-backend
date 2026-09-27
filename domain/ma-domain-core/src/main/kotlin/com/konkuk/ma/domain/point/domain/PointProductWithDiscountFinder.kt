package com.konkuk.ma.domain.point.domain

import com.konkuk.ma.domain.point.domain.port.DiscountPolicyQueryRepository
import com.konkuk.ma.domain.point.domain.port.PointProductCacheRepository
import com.konkuk.ma.domain.point.domain.port.PointProductQueryRepository
import com.konkuk.ma.logger
import org.springframework.stereotype.Component

@Component
class PointProductWithDiscountFinder(
    private val pointProductQueryRepository: PointProductQueryRepository,
    private val discountPolicyQueryRepository: DiscountPolicyQueryRepository,
    private val pointProductCacheRepository: PointProductCacheRepository,
) {
    fun findAll(): List<PointProductWithDiscount> {
        return findAllFromCache() ?: loadAllFromDbAndCache()
    }

    fun findOne(pointProductId: Long): PointProductWithDiscount {
        return findOneFromCache(pointProductId) ?: loadOneFromDb(pointProductId)
    }

    private fun loadAllFromDbAndCache(): List<PointProductWithDiscount> {
        val products = PointProducts(pointProductQueryRepository.find())
        val discountPolicies = discountPolicyQueryRepository.find(products.extractDiscountPolicyIds())
        val combined = products.combineWith(discountPolicies)
        saveToCache(combined.data)
        return combined.data
    }

    private fun loadOneFromDb(pointProductId: Long): PointProductWithDiscount {
        val product = pointProductQueryRepository.findOne(pointProductId)
        val discountPolicy = product.discountPolicyId?.let { discountPolicyQueryRepository.findOneOrNull(it) }
        return PointProductWithDiscount(product, discountPolicy)
    }

    private fun findAllFromCache(): List<PointProductWithDiscount>? {
        return runCatching { pointProductCacheRepository.findOrNull() }
            .onFailure { logger.error(it) { "Redis 포인트 상품 목록 캐시 조회 실패" } }
            .getOrNull()
    }

    private fun findOneFromCache(pointProductId: Long): PointProductWithDiscount? {
        return runCatching { pointProductCacheRepository.findOneOrNull(pointProductId) }
            .onFailure { logger.error(it) { "Redis 포인트 상품 단건 캐시 조회 실패 (pointProductId=$pointProductId)" } }
            .getOrNull()
    }

    private fun saveToCache(products: List<PointProductWithDiscount>) {
        runCatching { pointProductCacheRepository.save(products) }
            .onFailure { logger.error(it) { "Redis 포인트 상품 캐시 저장 실패 (size=${products.size})" } }
    }
}
