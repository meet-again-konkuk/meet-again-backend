package com.konkuk.ma.domain.point.domain.payment

import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.application.command.ChargePointCommand
import com.konkuk.ma.domain.point.domain.PointProductWithDiscount

class PaymentOrder(
    val paymentMethod: PaymentMethod,
    val paymentToken: String,
    val amount: Money,
    val idempotencyKey: String,
    val orderName: String,
) {
    companion object {
        fun of(
            command: ChargePointCommand,
            productWithDiscount: PointProductWithDiscount,
        ): PaymentOrder {
            return PaymentOrder(
                paymentMethod = command.paymentMethod,
                paymentToken = command.paymentToken,
                amount = productWithDiscount.discountedPrice(),
                idempotencyKey = command.idempotencyKey,
                orderName = productWithDiscount.productName(),
            )
        }
    }
}
