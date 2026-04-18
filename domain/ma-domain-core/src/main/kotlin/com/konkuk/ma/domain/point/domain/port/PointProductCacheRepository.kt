package com.konkuk.ma.domain.point.domain.port

import com.konkuk.ma.domain.point.domain.PointProductWithDiscount

interface PointProductCacheRepository {
    fun findOrNull(): List<PointProductWithDiscount>?
    fun save(products: List<PointProductWithDiscount>)
}
