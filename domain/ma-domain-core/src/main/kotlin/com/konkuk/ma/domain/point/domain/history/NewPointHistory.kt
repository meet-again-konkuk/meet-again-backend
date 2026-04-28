package com.konkuk.ma.domain.point.domain.history

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.application.command.ChargePointCommand
import com.konkuk.ma.domain.point.domain.PointProductWithDiscount
import com.konkuk.ma.domain.point.domain.balance.PointQuantity
import com.konkuk.ma.domain.point.domain.payment.PaymentApproval
import com.konkuk.ma.domain.point.domain.payment.PaymentMethod

class NewPointHistory(
    val ownerEmail: Email,
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
            command: ChargePointCommand,
            productWithDiscount: PointProductWithDiscount,
            approval: PaymentApproval,
        ): NewPointHistory {
            return NewPointHistory(
                ownerEmail = command.ownerEmail,
                pointProductId = productWithDiscount.productId(),
                historyType = PointHistoryType.CHARGE,
                quantity = productWithDiscount.chargeQuantity(),
                paidAmount = approval.approvedAmount,
                paymentMethod = command.paymentMethod,
                idempotencyKey = command.idempotencyKey,
                approvalNumber = approval.approvalNumber,
            )
        }
    }
}
