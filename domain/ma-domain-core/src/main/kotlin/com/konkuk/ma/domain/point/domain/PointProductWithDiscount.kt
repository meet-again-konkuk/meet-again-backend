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

    fun discountedPriceOrNull(now: LocalDate): Int? {
        if (!isDiscountActive(now)) return null
        return discountPolicy!!.calculateDiscountedPrice(pointProduct.price)
    }

    fun discountType(): DiscountType? = discountPolicy?.type
}
