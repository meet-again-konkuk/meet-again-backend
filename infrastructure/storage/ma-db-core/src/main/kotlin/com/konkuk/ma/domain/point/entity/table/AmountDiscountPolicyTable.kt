package com.konkuk.ma.domain.point.entity.table

import org.jetbrains.exposed.dao.id.LongIdTable

object AmountDiscountPolicyTable : LongIdTable("AMOUNT_DISCOUNT_POLICIES", "DISCOUNT_POLICY_ID") {
    val discountAmount = integer("DISCOUNT_AMOUNT")
}
