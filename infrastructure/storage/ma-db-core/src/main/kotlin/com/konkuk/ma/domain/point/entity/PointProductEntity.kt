package com.konkuk.ma.domain.point.entity

import com.konkuk.ma.domain.point.domain.PointProduct

class PointProductEntity(
    val id: Long,
    val name: String,
    val quantity: Int,
    val price: Int,
    val displayOrder: Int,
) {
    fun toDomain(): PointProduct {
        return PointProduct(
            pointProductId = id,
            name = name,
            quantity = quantity,
            price = price,
            displayOrder = displayOrder,
        )
    }
}
