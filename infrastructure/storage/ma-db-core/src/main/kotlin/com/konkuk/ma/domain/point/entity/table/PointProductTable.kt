package com.konkuk.ma.domain.point.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object PointProductTable : BaseTable("POINT_PRODUCTS", "POINT_PRODUCT_ID") {
    val name = varchar("NAME", 100)
    val quantity = integer("QUANTITY")
    val price = integer("PRICE")
    val displayOrder = integer("DISPLAY_ORDER")
    val discountPolicyId = long("DISCOUNT_POLICY_ID").nullable()
}
