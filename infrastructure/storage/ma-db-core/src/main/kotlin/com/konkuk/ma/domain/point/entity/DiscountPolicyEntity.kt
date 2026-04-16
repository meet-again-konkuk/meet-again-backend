package com.konkuk.ma.domain.point.entity

import com.konkuk.ma.domain.point.domain.discount.AmountDiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.DiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.PercentDiscountPolicy
import java.time.LocalDate

class DiscountPolicyEntity(
    val id: Long,
    val policyType: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val discountAmount: Int?,
    val discountPercent: Int?,
) {
    fun toDomain(): DiscountPolicy {
        return when (policyType) {
            "AMOUNT" -> AmountDiscountPolicy(
                discountPolicyId = id,
                startDate = startDate,
                endDate = endDate,
                discountAmount = discountAmount!!,
            )
            "PERCENT" -> PercentDiscountPolicy(
                discountPolicyId = id,
                startDate = startDate,
                endDate = endDate,
                discountPercent = discountPercent!!,
            )
            else -> throw IllegalArgumentException("알 수 없는 할인 정책 타입: $policyType")
        }
    }
}
