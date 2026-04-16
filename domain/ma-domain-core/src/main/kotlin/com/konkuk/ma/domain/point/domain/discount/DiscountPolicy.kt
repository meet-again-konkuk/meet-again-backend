package com.konkuk.ma.domain.point.domain.discount

import java.time.LocalDate

sealed class DiscountPolicy(
    val discountPolicyId: Long,
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    fun isActive(now: LocalDate): Boolean {
        return !now.isBefore(startDate) && !now.isAfter(endDate)
    }

    abstract fun calculateDiscountedPrice(price: Int): Int
}
