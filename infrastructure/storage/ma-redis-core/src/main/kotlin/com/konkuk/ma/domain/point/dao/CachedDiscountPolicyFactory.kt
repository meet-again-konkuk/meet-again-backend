package com.konkuk.ma.domain.point.dao

import com.konkuk.ma.domain.point.domain.discount.DiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.DiscountType
import com.konkuk.ma.domain.point.entity.CachedPointProductEntity

interface CachedDiscountPolicyFactory {
    val type: DiscountType

    fun create(cached: CachedPointProductEntity): DiscountPolicy

    fun serialize(policy: DiscountPolicy, base: CachedPointProductEntity): CachedPointProductEntity
}
