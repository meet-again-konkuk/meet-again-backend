package com.konkuk.ma.domain.point.domain.discount

import com.konkuk.ma.domain.common.domain.Money
import java.time.LocalDate

class AmountDiscountPolicy(
    discountPolicyId: Long,
    startDate: LocalDate,
    endDate: LocalDate,
    val discountAmount: Money,
) : DiscountPolicy(discountPolicyId, startDate, endDate) {

    override val type: DiscountType = DiscountType.AMOUNT

    override fun calculateDiscountedPrice(price: Money): Money {
        val discounted = price - discountAmount
        return if (discounted.isLessThan(Money.ZERO)) Money.ZERO else discounted
    }
}
