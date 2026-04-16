package com.konkuk.ma.domain.point.application

import com.konkuk.ma.domain.point.domain.PointProduct
import com.konkuk.ma.domain.point.domain.port.PointProductCacheRepository
import com.konkuk.ma.domain.point.domain.port.PointProductQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PointQueryService(
    private val pointProductQueryRepository: PointProductQueryRepository,
    private val pointProductCacheRepository: PointProductCacheRepository,
) {
    fun findProducts(): List<PointProduct> {
        val cached = pointProductCacheRepository.findOrNull()
        if (cached != null) return cached

        val products = pointProductQueryRepository.find()
        pointProductCacheRepository.save(products)
        return products
    }
}
