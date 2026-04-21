package com.konkuk.ma.domain.point.domain

import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.application.command.ChargePointCommand
import com.konkuk.ma.domain.point.domain.port.PointTransactionRepository
import com.konkuk.ma.exception.DuplicateException
import com.konkuk.ma.exception.EntityType
import com.konkuk.ma.exception.InvalidStateException
import org.springframework.stereotype.Component

@Component
class PointChargeValidator(
    private val pointTransactionRepository: PointTransactionRepository,
) {
    fun validate(command: ChargePointCommand, serverPrice: Money) {
        validateIdempotency(command.idempotencyKey)
        validatePrice(command.expectedPrice, serverPrice)
    }

    private fun validateIdempotency(idempotencyKey: String) {
        val existing = pointTransactionRepository.findOneOrNull(idempotencyKey)
        if (existing != null) {
            throw DuplicateException(EntityType.POINT_TRANSACTION, "idempotencyKey", idempotencyKey)
        }
    }

    private fun validatePrice(expectedPrice: Int, serverPrice: Money) {
        if (expectedPrice != serverPrice.toInt()) {
            throw InvalidStateException(
                PointChargeValidator::class,
                expectedPrice,
                "클라이언트가 기대한 가격($expectedPrice)과 서버 계산 가격(${serverPrice.toInt()})이 다릅니다.",
            )
        }
    }
}
