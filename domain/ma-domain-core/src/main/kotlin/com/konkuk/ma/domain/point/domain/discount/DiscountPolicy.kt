package com.konkuk.ma.domain.point.domain.discount

import com.konkuk.ma.domain.common.domain.Money
import java.time.LocalDate

sealed class DiscountPolicy(
    val discountPolicyId: Long,
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    abstract val type: DiscountType

    fun isActive(now: LocalDate): Boolean {
        return !now.isBefore(startDate) && !now.isAfter(endDate)
    }

    abstract fun calculateDiscountedPrice(price: Money): Money
}
