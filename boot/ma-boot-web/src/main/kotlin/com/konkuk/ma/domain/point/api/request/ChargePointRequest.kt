package com.konkuk.ma.domain.point.api.request

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.point.application.command.ChargePointCommand
import com.konkuk.ma.domain.point.domain.payment.PaymentMethod
import com.konkuk.ma.support.id.EncryptId
import com.konkuk.ma.support.validation.ValidationMessages
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

class ChargePointRequest(
    @field:NotNull(message = ValidationMessages.POINT_PRODUCT_ID_REQUIRED)
    @EncryptId(ObfuscationType.POINT_PRODUCT)
    val pointProductId: Long,

    @field:NotNull(message = ValidationMessages.PAYMENT_METHOD_REQUIRED)
    val paymentMethod: PaymentMethod,

    @field:NotBlank(message = ValidationMessages.PAYMENT_TOKEN_REQUIRED)
    val paymentToken: String,

    @field:Min(value = 0, message = ValidationMessages.EXPECTED_PRICE_INVALID)
    val expectedPrice: Int,

    @field:NotBlank(message = ValidationMessages.IDEMPOTENCY_KEY_REQUIRED)
    val idempotencyKey: String,
) {
    fun toCommand(email: String): ChargePointCommand {
        return ChargePointCommand(
            email = email,
            pointProductId = pointProductId,
            paymentMethod = paymentMethod,
            paymentToken = paymentToken,
            expectedPrice = expectedPrice,
            idempotencyKey = idempotencyKey,
        )
    }
}
