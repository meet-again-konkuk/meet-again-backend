package com.konkuk.ma.domain.point.domain.transaction

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.domain.balance.PointQuantity
import com.konkuk.ma.domain.point.domain.payment.PaymentMethod
import java.time.LocalDateTime

class PointTransaction(
    val id: Long,
    val ownerEmail: Email,
    val pointProductId: Long?,
    val transactionType: PointTransactionType,
    val quantity: PointQuantity,
    val paidAmount: Money,
    val paymentMethod: PaymentMethod?,
    val idempotencyKey: String,
    val approvalNumber: String?,
    val createdDate: LocalDateTime,
)
