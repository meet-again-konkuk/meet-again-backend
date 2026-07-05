package com.konkuk.ma.domain.point.domain.payment

import com.konkuk.ma.domain.common.domain.Money
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
            paymentMethod: PaymentMethod,
            paymentToken: String,
            idempotencyKey: String,
            productWithDiscount: PointProductWithDiscount,
        ): PaymentOrder {
            return PaymentOrder(
                paymentMethod = paymentMethod,
                paymentToken = paymentToken,
                amount = productWithDiscount.discountedPrice(),
                idempotencyKey = idempotencyKey,
                orderName = productWithDiscount.productName(),
            )
        }
    }
}
