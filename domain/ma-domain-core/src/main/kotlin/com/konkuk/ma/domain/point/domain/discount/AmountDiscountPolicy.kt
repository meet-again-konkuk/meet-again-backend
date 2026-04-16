package com.konkuk.ma.domain.point.domain.discount

import java.time.LocalDate

class AmountDiscountPolicy(
    discountPolicyId: Long,
    startDate: LocalDate,
    endDate: LocalDate,
    val discountAmount: Int,
) : DiscountPolicy(discountPolicyId, startDate, endDate) {

    override fun calculateDiscountedPrice(price: Int): Int {
        return maxOf(0, price - discountAmount)
    }
}
