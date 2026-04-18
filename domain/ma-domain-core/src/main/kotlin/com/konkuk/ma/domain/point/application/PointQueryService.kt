package com.konkuk.ma.domain.point.application

import com.konkuk.ma.domain.point.domain.PointProductProvider
import com.konkuk.ma.domain.point.domain.PointProductWithDiscount
import com.konkuk.ma.domain.point.domain.PointProducts
import com.konkuk.ma.domain.point.domain.port.DiscountPolicyQueryRepository
import com.konkuk.ma.domain.point.domain.port.PointProductQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PointQueryService(
    private val pointProductQueryRepository: PointProductQueryRepository,
    private val discountPolicyQueryRepository: DiscountPolicyQueryRepository,
    private val pointProductProvider: PointProductProvider,
) {
    fun findProducts(): List<PointProductWithDiscount> {
        val cached = pointProductProvider.findFromCache()
        if (cached != null) return cached

        val products = PointProducts(pointProductQueryRepository.find())
        val discountPolicies = discountPolicyQueryRepository.find(products.extractDiscountPolicyIds())
        val combined = products.combineWith(discountPolicies)

        pointProductProvider.saveToCache(combined.data)
        return combined.data
    }
}
