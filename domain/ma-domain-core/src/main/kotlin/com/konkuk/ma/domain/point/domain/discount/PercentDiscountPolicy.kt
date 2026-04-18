package com.konkuk.ma.domain.point.domain.discount

import java.time.LocalDate

class PercentDiscountPolicy(
    discountPolicyId: Long,
    startDate: LocalDate,
    endDate: LocalDate,
    val discountPercent: Int,
) : DiscountPolicy(discountPolicyId, startDate, endDate) {

    override val type: DiscountType = DiscountType.PERCENT

    override fun calculateDiscountedPrice(price: Int): Int {
        return price * (100 - discountPercent) / 100
    }
}
