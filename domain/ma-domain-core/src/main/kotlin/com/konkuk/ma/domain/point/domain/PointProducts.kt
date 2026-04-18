package com.konkuk.ma.domain.point.domain

import com.konkuk.ma.domain.point.domain.discount.DiscountPolicy

class PointProducts(
    val data: List<PointProduct>,
) {
    fun extractDiscountPolicyIds(): Set<Long> {
        return data.mapNotNull { it.discountPolicyId }.toSet()
    }

    fun combineWith(discountPolicies: List<DiscountPolicy>): PointProducts {
        val policyMap = discountPolicies.associateBy { it.discountPolicyId }
        return PointProducts(
            data.map { product -> product.withDiscountPolicy(policyMap[product.discountPolicyId]) }
        )
    }
}
