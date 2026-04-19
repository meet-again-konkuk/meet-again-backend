package com.konkuk.ma.domain.point.domain.discount

import com.konkuk.ma.domain.common.domain.Money
import java.time.LocalDate

class PercentDiscountPolicy(
    discountPolicyId: Long,
    startDate: LocalDate,
    endDate: LocalDate,
    val discountPercent: Int,
) : DiscountPolicy(discountPolicyId, startDate, endDate) {

    override val type: DiscountType = DiscountType.PERCENT

    override fun calculateDiscountedPrice(price: Money): Money {
        return price.times(100 - discountPercent, 100)
    }
}
