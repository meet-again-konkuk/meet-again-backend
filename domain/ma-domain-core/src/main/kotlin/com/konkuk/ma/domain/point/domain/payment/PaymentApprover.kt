package com.konkuk.ma.domain.point.domain.payment

interface PaymentApprover {
    fun supports(method: PaymentMethod): Boolean

    fun approve(request: PaymentApprovalRequest): PaymentApproval

    fun cancel(approvalNumber: String, reason: String)
}
