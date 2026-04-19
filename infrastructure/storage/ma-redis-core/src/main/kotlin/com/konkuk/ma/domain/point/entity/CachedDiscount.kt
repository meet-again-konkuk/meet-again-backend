package com.konkuk.ma.domain.point.entity

import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.domain.discount.AmountDiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.DiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.PercentDiscountPolicy
import java.time.LocalDate

sealed class CachedDiscount {
    abstract fun toDomain(): DiscountPolicy
}

data class CachedAmountDiscount(
    val startDate: String = "",
    val endDate: String = "",
    val discountAmount: Int = 0,
) : CachedDiscount() {
    override fun toDomain(): DiscountPolicy {
        return AmountDiscountPolicy(
            discountPolicyId = 0,
            startDate = LocalDate.parse(startDate),
            endDate = LocalDate.parse(endDate),
            discountAmount = Money.wons(discountAmount),
        )
    }
}

data class CachedPercentDiscount(
    val startDate: String = "",
    val endDate: String = "",
    val discountPercent: Int = 0,
) : CachedDiscount() {
    override fun toDomain(): DiscountPolicy {
        return PercentDiscountPolicy(
            discountPolicyId = 0,
            startDate = LocalDate.parse(startDate),
            endDate = LocalDate.parse(endDate),
            discountPercent = discountPercent,
        )
    }
}
