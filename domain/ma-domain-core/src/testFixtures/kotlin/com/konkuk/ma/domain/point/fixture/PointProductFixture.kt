package com.konkuk.ma.domain.point.fixture

import com.konkuk.ma.domain.point.domain.PointProduct

object PointProductFixture {
    fun create(
        pointProductId: Long = 1L,
        name: String = "인연 10개",
        quantity: Int = 10,
        price: Int = 1000,
        displayOrder: Int = 1,
    ): PointProduct {
        return PointProduct(
            pointProductId = pointProductId,
            name = name,
            quantity = quantity,
            price = price,
            displayOrder = displayOrder,
        )
    }
}
