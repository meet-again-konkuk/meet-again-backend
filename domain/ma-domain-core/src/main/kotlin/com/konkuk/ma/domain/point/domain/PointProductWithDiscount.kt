package com.konkuk.ma.domain.point.domain

import com.konkuk.ma.domain.common.domain.Money
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

    fun discountedPrice(now: LocalDate): Money {
        if (!isDiscountActive(now)) return pointProduct.price
        return discountPolicy!!.calculateDiscountedPrice(pointProduct.price)
    }

    fun discountRate(now: LocalDate): Int {
        if (!isDiscountActive(now)) return 0
        val price = pointProduct.price
        if (price.isZero()) return 0
        val discounted = discountedPrice(now)
        return ((price - discounted).toLong() * 100 / price.toLong()).toInt()
    }

    fun discountType(): DiscountType? = discountPolicy?.type
}
