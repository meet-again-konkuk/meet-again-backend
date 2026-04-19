package com.konkuk.ma.domain.point.dao

import com.konkuk.ma.domain.point.domain.discount.DiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.DiscountType

interface CachedDiscountPolicyFactory {
    val type: DiscountType

    fun create(cached: CachedPointProduct): DiscountPolicy
}
