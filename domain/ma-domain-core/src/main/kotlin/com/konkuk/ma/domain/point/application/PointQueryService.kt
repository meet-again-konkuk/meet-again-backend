package com.konkuk.ma.domain.point.application

import com.konkuk.ma.domain.point.domain.PointProductWithDiscount
import com.konkuk.ma.domain.point.domain.PointProductWithDiscountFinder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PointQueryService(
    private val productFinder: PointProductWithDiscountFinder,
) {
    fun findProducts(): List<PointProductWithDiscount> {
        return productFinder.findAll()
    }
}
