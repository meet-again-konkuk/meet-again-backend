package com.konkuk.ma.domain.point.application.command

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.point.domain.payment.PaymentMethod

class ChargePointCommand(
    email: String,
    val pointProductId: Long,
    val paymentMethod: PaymentMethod,
    val paymentToken: String,
    val orderPointPrice: Int,
    val idempotencyKey: String,
) {
    val ownerEmail: Email = Email(email)
}
