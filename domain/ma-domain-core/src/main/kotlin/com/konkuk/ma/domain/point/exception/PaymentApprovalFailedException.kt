package com.konkuk.ma.domain.point.exception

import com.konkuk.ma.exception.BusinessException

class PaymentApprovalFailedException(
    paymentToken: String,
    reason: String,
) : BusinessException(
    message = MESSAGE,
    dataMessage = "paymentToken: $paymentToken, reason: $reason",
    logLevel = LogLevel.WARN,
) {
    companion object {
        const val MESSAGE = "결제 승인에 실패했습니다."
    }
}
