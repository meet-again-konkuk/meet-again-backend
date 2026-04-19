package com.konkuk.ma.domain.point.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable
import org.jetbrains.exposed.sql.javatime.date

object DiscountPolicyTable : BaseTable("DISCOUNT_POLICIES", "DISCOUNT_POLICY_ID") {
    val policyType = varchar("POLICY_TYPE", 20)
    val startDate = date("START_DATE")
    val endDate = date("END_DATE")
}
