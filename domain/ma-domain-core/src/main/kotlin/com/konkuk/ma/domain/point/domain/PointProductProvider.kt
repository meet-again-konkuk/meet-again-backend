package com.konkuk.ma.domain.point.domain

import com.konkuk.ma.domain.point.domain.port.PointProductCacheRepository
import com.konkuk.ma.domain.point.domain.port.PointProductQueryRepository
import com.konkuk.ma.logger
import org.springframework.stereotype.Component

@Component
class PointProductProvider(
    private val pointProductCacheRepository: PointProductCacheRepository,
    private val pointProductQueryRepository: PointProductQueryRepository,
) {
    fun find(): List<PointProduct> {
        val cached = findFromCache()
        if (cached != null) return cached

        val products = pointProductQueryRepository.find()
        saveToCache(products)
        return products
    }

    private fun findFromCache(): List<PointProduct>? {
        return runCatching { pointProductCacheRepository.findOrNull() }
            .onFailure { logger.warn { "Redis 캐시 조회 실패: ${it.message}" } }
            .getOrNull()
    }

    private fun saveToCache(products: List<PointProduct>) {
        runCatching { pointProductCacheRepository.save(products) }
            .onFailure { logger.warn { "Redis 캐시 저장 실패: ${it.message}" } }
    }
}
