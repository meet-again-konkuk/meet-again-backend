package com.konkuk.ma.domain.point.application

import com.konkuk.ma.domain.point.domain.PointProduct
import com.konkuk.ma.domain.point.domain.port.PointProductCacheRepository
import com.konkuk.ma.domain.point.domain.port.PointProductQueryRepository
import com.konkuk.ma.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PointQueryService(
    private val pointProductQueryRepository: PointProductQueryRepository,
    private val pointProductCacheRepository: PointProductCacheRepository,
) {
    fun findProducts(): List<PointProduct> {
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
