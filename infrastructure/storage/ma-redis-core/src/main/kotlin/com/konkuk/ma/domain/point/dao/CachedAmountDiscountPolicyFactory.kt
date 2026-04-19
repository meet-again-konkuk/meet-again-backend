package com.konkuk.ma.domain.point.dao

import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.domain.discount.AmountDiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.DiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.DiscountType
import com.konkuk.ma.domain.point.entity.CachedPointProductEntity
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class CachedAmountDiscountPolicyFactory : CachedDiscountPolicyFactory {
    override val type: DiscountType = DiscountType.AMOUNT

    override fun create(cached: CachedPointProductEntity): DiscountPolicy {
        return AmountDiscountPolicy(
            discountPolicyId = 0,
            startDate = LocalDate.parse(requireNotNull(cached.discountStartDate)),
            endDate = LocalDate.parse(requireNotNull(cached.discountEndDate)),
            discountAmount = Money.wons(
                requireNotNull(cached.discountAmount) { "AMOUNT 정책은 discountAmount가 필수입니다" }
            ),
        )
    }

    override fun serialize(policy: DiscountPolicy, base: CachedPointProductEntity): CachedPointProductEntity {
        policy as AmountDiscountPolicy
        return base.copy(
            discountType = type.name,
            discountAmount = policy.discountAmount.toInt(),
            discountStartDate = policy.startDate.toString(),
            discountEndDate = policy.endDate.toString(),
        )
    }
}
