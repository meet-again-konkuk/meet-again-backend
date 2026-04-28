package com.konkuk.ma.domain.point.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object PointHistoryTable : BaseTable("POINT_HISTORIES", "POINT_HISTORY_ID") {
    val ownerEmail = varchar("OWNER_EMAIL", 255)
    val pointProductId = long("POINT_PRODUCT_ID").nullable()
    val historyType = varchar("HISTORY_TYPE", 16)
    val quantity = integer("QUANTITY")
    val paidAmount = integer("PAID_AMOUNT")
    val paymentMethod = varchar("PAYMENT_METHOD", 32).nullable()
    val idempotencyKey = varchar("IDEMPOTENCY_KEY", 64).uniqueIndex()
    val approvalNumber = varchar("APPROVAL_NUMBER", 64).nullable()
}
