package com.konkuk.ma.domain.point.domain.history

import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.domain.PointProductWithDiscount
import com.konkuk.ma.domain.point.domain.balance.PointQuantity
import com.konkuk.ma.domain.point.domain.payment.PaymentApproval
import com.konkuk.ma.domain.point.domain.payment.PaymentMethod

class NewPointHistory(
    val ownerId: Long,
    val pointProductId: Long?,
    val historyType: PointHistoryType,
    val quantity: PointQuantity,
    val paidAmount: Money,
    val paymentMethod: PaymentMethod?,
    val idempotencyKey: String,
    val approvalNumber: String?,
) {
    companion object {
        fun forCharge(
            ownerId: Long,
            paymentMethod: PaymentMethod,
            idempotencyKey: String,
            productWithDiscount: PointProductWithDiscount,
            approval: PaymentApproval,
        ): NewPointHistory {
            return NewPointHistory(
                ownerId = ownerId,
                pointProductId = productWithDiscount.productId(),
                historyType = PointHistoryType.CHARGE,
                quantity = productWithDiscount.chargeQuantity(),
                paidAmount = approval.approvedAmount,
                paymentMethod = paymentMethod,
                idempotencyKey = idempotencyKey,
                approvalNumber = approval.approvalNumber,
            )
        }
    }
}
