package com.konkuk.ma.domain.point.dao

import com.konkuk.ma.domain.point.domain.discount.DiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.DiscountType
import com.konkuk.ma.domain.point.domain.discount.PercentDiscountPolicy
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class CachedPercentDiscountPolicyFactory : CachedDiscountPolicyFactory {
    override val type: DiscountType = DiscountType.PERCENT

    override fun create(cached: CachedPointProduct): DiscountPolicy {
        return PercentDiscountPolicy(
            discountPolicyId = 0,
            startDate = LocalDate.parse(requireNotNull(cached.discountStartDate)),
            endDate = LocalDate.parse(requireNotNull(cached.discountEndDate)),
            discountPercent = requireNotNull(cached.discountPercent) { "PERCENT 정책은 discountPercent가 필수입니다" },
        )
    }

    override fun serialize(policy: DiscountPolicy, base: CachedPointProduct): CachedPointProduct {
        policy as PercentDiscountPolicy
        return base.copy(
            discountType = type.name,
            discountPercent = policy.discountPercent,
            discountStartDate = policy.startDate.toString(),
            discountEndDate = policy.endDate.toString(),
        )
    }
}
