package com.konkuk.ma.domain.point.entity

import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.domain.discount.AmountDiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.DiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.PercentDiscountPolicy
import java.time.LocalDate

sealed class DiscountPolicyEntity(
    val id: Long,
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    abstract fun toDomain(): DiscountPolicy
}

class AmountDiscountPolicyEntity(
    id: Long,
    startDate: LocalDate,
    endDate: LocalDate,
    val discountAmount: Int,
) : DiscountPolicyEntity(id, startDate, endDate) {
    override fun toDomain(): DiscountPolicy {
        return AmountDiscountPolicy(
            discountPolicyId = id,
            startDate = startDate,
            endDate = endDate,
            discountAmount = Money.wons(discountAmount),
        )
    }
}

class PercentDiscountPolicyEntity(
    id: Long,
    startDate: LocalDate,
    endDate: LocalDate,
    val discountPercent: Int,
) : DiscountPolicyEntity(id, startDate, endDate) {
    override fun toDomain(): DiscountPolicy {
        return PercentDiscountPolicy(
            discountPolicyId = id,
            startDate = startDate,
            endDate = endDate,
            discountPercent = discountPercent,
        )
    }
}
