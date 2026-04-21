package com.konkuk.ma.domain.point.domain.port

import com.konkuk.ma.domain.point.domain.PointProduct

interface PointProductQueryRepository {
    fun find(): List<PointProduct>

    fun findOne(id: Long): PointProduct
}
