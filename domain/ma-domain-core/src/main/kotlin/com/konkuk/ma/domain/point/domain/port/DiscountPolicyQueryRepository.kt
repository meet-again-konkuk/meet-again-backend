package com.konkuk.ma.domain.point.domain.port

import com.konkuk.ma.domain.point.domain.discount.DiscountPolicy

interface DiscountPolicyQueryRepository {
    fun find(ids: Set<Long>): List<DiscountPolicy>
}
