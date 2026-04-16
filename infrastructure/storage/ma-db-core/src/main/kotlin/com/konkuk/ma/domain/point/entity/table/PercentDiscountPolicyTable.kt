package com.konkuk.ma.domain.point.entity.table

import org.jetbrains.exposed.dao.id.LongIdTable

object PercentDiscountPolicyTable : LongIdTable("PERCENT_DISCOUNT_POLICIES", "DISCOUNT_POLICY_ID") {
    val discountPercent = integer("DISCOUNT_PERCENT")
}
