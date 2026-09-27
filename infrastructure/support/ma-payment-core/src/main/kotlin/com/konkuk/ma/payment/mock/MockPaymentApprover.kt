package com.konkuk.ma.payment.mock

import com.konkuk.ma.domain.point.domain.payment.PaymentApproval
import com.konkuk.ma.domain.point.domain.payment.PaymentApprover
import com.konkuk.ma.domain.point.domain.payment.PaymentMethod
import com.konkuk.ma.domain.point.domain.payment.PaymentOrder
import com.konkuk.ma.domain.point.exception.PaymentApprovalFailedException
import com.konkuk.ma.logger
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("local", "test")
class MockPaymentApprover : PaymentApprover {
    override fun supports(method: PaymentMethod): Boolean = true

    override fun approve(order: PaymentOrder): PaymentApproval {
        if (order.paymentToken.startsWith(FAIL_TOKEN_PREFIX)) {
            throw PaymentApprovalFailedException(
                paymentToken = order.paymentToken,
                reason = "Mock 결제 승인이 FAIL 토큰으로 거부되었습니다.",
            )
        }
        return PaymentApproval(
            approvalNumber = "$APPROVAL_PREFIX${UUID.randomUUID()}",
            approvedAmount = order.amount,
            approvedAt = LocalDateTime.now(),
            paymentMethod = order.paymentMethod,
        )
    }

    override fun cancel(approvalNumber: String, reason: String) {
        logger.info { "Mock 결제 취소: approvalNumber=$approvalNumber, reason=$reason" }
    }

    companion object {
        private const val FAIL_TOKEN_PREFIX = "FAIL"
        private const val APPROVAL_PREFIX = "MOCK-"
    }
}
