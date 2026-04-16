package com.konkuk.ma.domain.point.domain.port

import com.konkuk.ma.domain.point.domain.PointProduct

interface PointProductCacheRepository {
    fun findOrNull(): List<PointProduct>?
    fun save(products: List<PointProduct>)
}
