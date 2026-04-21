package com.konkuk.ma.domain.point.domain.payment

import com.konkuk.ma.exception.InvalidStateException
import org.springframework.stereotype.Component

@Component
class PaymentApproverRouter(
    private val approvers: List<PaymentApprover>,
) {
    fun resolve(method: PaymentMethod): PaymentApprover {
        return approvers.find { it.supports(method) }
            ?: throw InvalidStateException(
                PaymentApproverRouter::class,
                method,
                "지원하지 않는 결제수단: $method",
            )
    }
}
