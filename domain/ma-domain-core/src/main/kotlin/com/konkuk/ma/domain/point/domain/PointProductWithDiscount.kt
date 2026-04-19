package com.konkuk.ma.domain.point.domain

import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.domain.discount.DiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.DiscountType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

class PointProductWithDiscount(
    val pointProduct: PointProduct,
    val discountPolicy: DiscountPolicy?,
) {
    fun isDiscountActive(now: LocalDate = LocalDate.now()): Boolean {
        return discountPolicy?.isActive(now) == true
    }

    fun discountedPrice(now: LocalDate = LocalDate.now()): Money {
        if (!isDiscountActive(now)) return pointProduct.price
        return discountPolicy!!.calculateDiscountedPrice(pointProduct.price)
    }

    fun discountRate(now: LocalDate = LocalDate.now()): BigDecimal {
        if (!isDiscountActive(now)) return BigDecimal.ZERO
        val price = pointProduct.price
        val discounted = discountedPrice(now)
        return (price - discounted).percentageOf(price).setScale(0, RoundingMode.HALF_UP)
    }

    fun discountType(): DiscountType? = discountPolicy?.type
}
