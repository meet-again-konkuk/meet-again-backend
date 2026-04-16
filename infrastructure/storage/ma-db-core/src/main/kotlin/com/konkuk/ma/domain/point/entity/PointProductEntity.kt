package com.konkuk.ma.domain.point.entity

import com.konkuk.ma.domain.point.domain.PointProduct
import com.konkuk.ma.domain.point.domain.discount.DiscountPolicy

class PointProductEntity(
    val id: Long,
    val name: String,
    val quantity: Int,
    val price: Int,
    val displayOrder: Int,
    val discountPolicyId: Long? = null,
) {
    fun toDomain(): PointProduct {
        return toDomain(null)
    }

    fun toDomain(discountPolicy: DiscountPolicy?): PointProduct {
        return PointProduct(
            pointProductId = id,
            name = name,
            quantity = quantity,
            price = price,
            displayOrder = displayOrder,
            discountPolicy = discountPolicy,
        )
    }
}
