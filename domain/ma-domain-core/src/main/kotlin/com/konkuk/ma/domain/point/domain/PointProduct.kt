package com.konkuk.ma.domain.point.domain

import com.konkuk.ma.domain.point.domain.discount.DiscountPolicy
import java.time.LocalDate

class PointProduct(
    val pointProductId: Long,
    val name: String,
    val quantity: Int,
    val price: Int,
    val displayOrder: Int,
    val discountPolicy: DiscountPolicy? = null,
) {
    fun discountedPrice(now: LocalDate): Int {
        if (discountPolicy == null || !discountPolicy.isActive(now)) return price
        return discountPolicy.calculateDiscountedPrice(price)
    }
}
