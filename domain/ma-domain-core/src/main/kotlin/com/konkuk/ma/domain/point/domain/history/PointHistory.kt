package com.konkuk.ma.domain.point.domain.history

import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.domain.balance.PointQuantity
import com.konkuk.ma.domain.point.domain.payment.PaymentMethod
import java.time.LocalDateTime

class PointHistory(
    val id: Long,
    val ownerId: Long,
    val pointProductId: Long?,
    val historyType: PointHistoryType,
    val quantity: PointQuantity,
    val paidAmount: Money,
    val paymentMethod: PaymentMethod?,
    val idempotencyKey: String,
    val approvalNumber: String?,
    val createdDate: LocalDateTime,
)
