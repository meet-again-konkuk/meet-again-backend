package com.konkuk.ma.domain.point.application.command

import com.konkuk.ma.domain.point.domain.payment.PaymentMethod

class ChargePointCommand(
    val ownerId: Long,
    val pointProductId: Long,
    val paymentMethod: PaymentMethod,
    val paymentToken: String,
    val orderPointPrice: Int,
    val idempotencyKey: String,
)
