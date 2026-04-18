package com.konkuk.ma.domain.point.domain

import com.konkuk.ma.domain.point.domain.discount.DiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.DiscountType
import java.time.LocalDate

class PointProduct(
    val pointProductId: Long,
    val name: String,
    val quantity: Int,
    val price: Int,
    val displayOrder: Int,
    val discountPolicy: DiscountPolicy? = null,
) {
    fun isDiscountActive(now: LocalDate): Boolean {
        return discountPolicy?.isActive(now) == true
    }

    fun discountedPrice(now: LocalDate): Int {
        if (!isDiscountActive(now)) return price
        return discountPolicy!!.calculateDiscountedPrice(price)
    }

    fun discountedPriceOrNull(now: LocalDate): Int? {
        if (!isDiscountActive(now)) return null
        return discountPolicy!!.calculateDiscountedPrice(price)
    }

    fun discountType(): DiscountType? = discountPolicy?.type
}
