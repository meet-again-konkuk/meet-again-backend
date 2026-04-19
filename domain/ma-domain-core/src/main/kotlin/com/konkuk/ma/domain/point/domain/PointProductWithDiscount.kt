package com.konkuk.ma.domain.point.domain

import com.konkuk.ma.domain.point.domain.discount.DiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.DiscountType
import java.time.LocalDate

class PointProductWithDiscount(
    val pointProduct: PointProduct,
    val discountPolicy: DiscountPolicy?,
) {
    fun isDiscountActive(now: LocalDate): Boolean {
        return discountPolicy?.isActive(now) == true
    }

    fun discountedPrice(now: LocalDate): Int {
        if (!isDiscountActive(now)) return pointProduct.price
        return discountPolicy!!.calculateDiscountedPrice(pointProduct.price)
    }

    fun discountRate(now: LocalDate): Int {
        if (!isDiscountActive(now)) return 0
        val price = pointProduct.price
        if (price == 0) return 0
        return (price - discountedPrice(now)) * 100 / price
    }

    fun discountType(): DiscountType? = discountPolicy?.type
}
