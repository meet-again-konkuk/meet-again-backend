package com.konkuk.ma.domain.point.api.request

import com.konkuk.ma.domain.point.application.command.ChargePointCommand
import com.konkuk.ma.domain.point.domain.payment.PaymentMethod
import com.konkuk.ma.support.validation.ValidationMessages
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

class ChargePointRequest(
    @field:NotNull(message = ValidationMessages.POINT_PRODUCT_ID_REQUIRED)
    val pointProductId: Long,

    @field:NotNull(message = ValidationMessages.PAYMENT_METHOD_REQUIRED)
    val paymentMethod: PaymentMethod,

    @field:NotBlank(message = ValidationMessages.PAYMENT_TOKEN_REQUIRED)
    val paymentToken: String,

    @field:Min(value = 0, message = ValidationMessages.ORDER_POINT_PRICE_INVALID)
    val orderPointPrice: Int,

    @field:NotBlank(message = ValidationMessages.IDEMPOTENCY_KEY_REQUIRED)
    val idempotencyKey: String,
) {
    fun toCommand(memberId: Long): ChargePointCommand {
        return ChargePointCommand(
            ownerId = memberId,
            pointProductId = pointProductId,
            paymentMethod = paymentMethod,
            paymentToken = paymentToken,
            orderPointPrice = orderPointPrice,
            idempotencyKey = idempotencyKey,
        )
    }
}
