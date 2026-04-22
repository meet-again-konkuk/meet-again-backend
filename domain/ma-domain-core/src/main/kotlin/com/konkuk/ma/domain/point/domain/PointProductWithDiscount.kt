package com.konkuk.ma.domain.point.domain

import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.domain.balance.PointQuantity
import com.konkuk.ma.domain.point.domain.discount.DiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.DiscountType
import com.konkuk.ma.exception.InvalidStateException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

class PointProductWithDiscount(
    val pointProduct: PointProduct,
    val discountPolicy: DiscountPolicy?,
) {
    fun productId(): Long = pointProduct.pointProductId

    fun productName(): String = pointProduct.name

    fun chargeQuantity(): PointQuantity = pointProduct.quantity

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

    fun verifyOrderPrice(orderPointPrice: Int, now: LocalDate = LocalDate.now()) {
        val pointPrice = discountedPrice(now)
        val orderPrice = Money.wons(orderPointPrice)
        if (orderPrice != pointPrice) {
            throw InvalidStateException(
                PointProductWithDiscount::class,
                orderPointPrice,
                "주문 가격($orderPrice)과 상품 가격($pointPrice)이 다릅니다.",
            )
        }
    }
}
