package com.konkuk.ma.domain.point.fixture

import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.domain.discount.AmountDiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.PercentDiscountPolicy
import java.time.LocalDate

object DiscountPolicyFixture {
    fun createAmount(
        discountPolicyId: Long = 1L,
        startDate: LocalDate = LocalDate.now().minusDays(1),
        endDate: LocalDate = LocalDate.now().plusDays(30),
        discountAmount: Int = 500,
    ): AmountDiscountPolicy {
        return AmountDiscountPolicy(
            discountPolicyId = discountPolicyId,
            startDate = startDate,
            endDate = endDate,
            discountAmount = Money.wons(discountAmount),
        )
    }

    fun createPercent(
        discountPolicyId: Long = 2L,
        startDate: LocalDate = LocalDate.now().minusDays(1),
        endDate: LocalDate = LocalDate.now().plusDays(30),
        discountPercent: Int = 10,
    ): PercentDiscountPolicy {
        return PercentDiscountPolicy(
            discountPolicyId = discountPolicyId,
            startDate = startDate,
            endDate = endDate,
            discountPercent = discountPercent,
        )
    }
}
