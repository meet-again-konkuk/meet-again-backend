package com.konkuk.ma.domain.point.entity

import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.domain.discount.AmountDiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.DiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.DiscountType
import com.konkuk.ma.domain.point.domain.discount.PercentDiscountPolicy
import java.time.LocalDate
import kotlin.reflect.full.createInstance

sealed class CachedDiscount {
    abstract val type: DiscountType
    abstract fun toDomain(): DiscountPolicy
    abstract fun fromDomain(policy: DiscountPolicy): CachedDiscount

    companion object {
        private val templates: List<CachedDiscount> by lazy {
            CachedDiscount::class.sealedSubclasses.map { it.createInstance() }
        }

        fun fromPolicy(policy: DiscountPolicy): CachedDiscount {
            return templates.find { it.type == policy.type }?.fromDomain(policy)
                ?: error("등록되지 않은 할인 정책 타입: ${policy.type}")
        }
    }
}

data class CachedAmountDiscount(
    val startDate: String = "",
    val endDate: String = "",
    val discountAmount: Int = 0,
) : CachedDiscount() {
    override val type: DiscountType = DiscountType.AMOUNT

    override fun toDomain(): DiscountPolicy {
        return AmountDiscountPolicy(
            discountPolicyId = 0,
            startDate = LocalDate.parse(startDate),
            endDate = LocalDate.parse(endDate),
            discountAmount = Money.wons(discountAmount),
        )
    }

    override fun fromDomain(policy: DiscountPolicy): CachedDiscount {
        policy as AmountDiscountPolicy
        return copy(
            startDate = policy.startDate.toString(),
            endDate = policy.endDate.toString(),
            discountAmount = policy.discountAmount.toInt(),
        )
    }
}

data class CachedPercentDiscount(
    val startDate: String = "",
    val endDate: String = "",
    val discountPercent: Int = 0,
) : CachedDiscount() {
    override val type: DiscountType = DiscountType.PERCENT

    override fun toDomain(): DiscountPolicy {
        return PercentDiscountPolicy(
            discountPolicyId = 0,
            startDate = LocalDate.parse(startDate),
            endDate = LocalDate.parse(endDate),
            discountPercent = discountPercent,
        )
    }

    override fun fromDomain(policy: DiscountPolicy): CachedDiscount {
        policy as PercentDiscountPolicy
        return copy(
            startDate = policy.startDate.toString(),
            endDate = policy.endDate.toString(),
            discountPercent = policy.discountPercent,
        )
    }
}
