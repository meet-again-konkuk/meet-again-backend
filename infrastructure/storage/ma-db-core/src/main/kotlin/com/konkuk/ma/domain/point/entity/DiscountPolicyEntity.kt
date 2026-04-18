package com.konkuk.ma.domain.point.entity

import com.konkuk.ma.domain.point.domain.discount.AmountDiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.DiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.DiscountType
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
        return when (DiscountType.valueOf(policyType)) {
            DiscountType.AMOUNT -> AmountDiscountPolicy(
                discountPolicyId = id,
                startDate = startDate,
                endDate = endDate,
                discountAmount = requireNotNull(discountAmount) { "AMOUNT 정책은 discountAmount가 필수입니다" },
            )
            DiscountType.PERCENT -> PercentDiscountPolicy(
                discountPolicyId = id,
                startDate = startDate,
                endDate = endDate,
                discountPercent = requireNotNull(discountPercent) { "PERCENT 정책은 discountPercent가 필수입니다" },
            )
        }
    }
}
