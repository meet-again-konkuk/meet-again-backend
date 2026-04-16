package com.konkuk.ma.domain.point.application

import com.konkuk.ma.domain.point.domain.PointProduct
import com.konkuk.ma.domain.point.domain.PointProductProvider
import com.konkuk.ma.domain.point.domain.port.PointProductQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PointQueryService(
    private val pointProductQueryRepository: PointProductQueryRepository,
    private val pointProductProvider: PointProductProvider,
) {
    fun findProducts(): List<PointProduct> {
        val cached = pointProductProvider.findFromCache()
        if (cached != null) return cached

        val products = pointProductQueryRepository.find()
        pointProductProvider.saveToCache(products)
        return products
    }
}
