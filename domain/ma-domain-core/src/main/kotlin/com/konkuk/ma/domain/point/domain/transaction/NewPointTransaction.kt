package com.konkuk.ma.domain.point.domain.transaction

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.application.command.ChargePointCommand
import com.konkuk.ma.domain.point.domain.PointProductWithDiscount
import com.konkuk.ma.domain.point.domain.balance.PointQuantity
import com.konkuk.ma.domain.point.domain.payment.PaymentApproval
import com.konkuk.ma.domain.point.domain.payment.PaymentMethod

class NewPointTransaction(
    val ownerEmail: Email,
    val pointProductId: Long?,
    val transactionType: PointTransactionType,
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
        ): NewPointTransaction {
            return NewPointTransaction(
                ownerEmail = command.ownerEmail,
                pointProductId = productWithDiscount.productId(),
                transactionType = PointTransactionType.CHARGE,
                quantity = productWithDiscount.chargeQuantity(),
                paidAmount = approval.approvedAmount,
                paymentMethod = command.paymentMethod,
                idempotencyKey = command.idempotencyKey,
                approvalNumber = approval.approvalNumber,
            )
        }
    }
}
