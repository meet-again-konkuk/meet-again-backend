package com.konkuk.ma.domain.point.domain

import com.konkuk.ma.domain.point.domain.port.PointProductCacheRepository
import com.konkuk.ma.logger
import org.springframework.stereotype.Component

@Component
class PointProductProvider(
    private val pointProductCacheRepository: PointProductCacheRepository,
) {
    fun findFromCache(): List<PointProductWithDiscount>? {
        return runCatching { pointProductCacheRepository.findOrNull() }
            .onFailure { logger.error { "Redis 캐시 조회 실패: ${it.message}" } }
            .getOrNull()
    }

    fun saveToCache(products: List<PointProductWithDiscount>) {
        runCatching { pointProductCacheRepository.save(products) }
            .onFailure { logger.error { "Redis 캐시 저장 실패: ${it.message}" } }
    }
}
