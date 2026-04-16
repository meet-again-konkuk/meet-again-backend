package com.konkuk.ma.domain.point.fixture

import com.konkuk.ma.domain.point.domain.PointProduct
import com.konkuk.ma.domain.point.domain.discount.DiscountPolicy

object PointProductFixture {
    fun create(
        pointProductId: Long = 1L,
        name: String = "인연 10개",
        quantity: Int = 10,
        price: Int = 1000,
        displayOrder: Int = 1,
        discountPolicy: DiscountPolicy? = null,
    ): PointProduct {
        return PointProduct(
            pointProductId = pointProductId,
            name = name,
            quantity = quantity,
            price = price,
            displayOrder = displayOrder,
            discountPolicy = discountPolicy,
        )
    }
}
