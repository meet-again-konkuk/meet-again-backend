package com.konkuk.ma.domain.point.domain.payment

import com.konkuk.ma.domain.common.domain.Money
import java.time.LocalDateTime

class PaymentApproval(
    val approvalNumber: String,
    val approvedAmount: Money,
    val approvedAt: LocalDateTime,
    val paymentMethod: PaymentMethod,
) {
    fun matchesAmount(expected: Money): Boolean = approvedAmount == expected
}
