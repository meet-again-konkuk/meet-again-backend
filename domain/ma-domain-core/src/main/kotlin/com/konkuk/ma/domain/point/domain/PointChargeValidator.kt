package com.konkuk.ma.domain.point.domain

import com.konkuk.ma.domain.point.domain.port.PointHistoryRepository
import com.konkuk.ma.exception.DuplicateException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Component

@Component
class PointChargeValidator(
    private val pointHistoryRepository: PointHistoryRepository,
) {
    fun validate(idempotencyKey: String, orderPointPrice: Int, product: PointProductWithDiscount) {
        validateIdempotency(idempotencyKey)
        product.verifyOrderPrice(orderPointPrice)
    }

    private fun validateIdempotency(idempotencyKey: String) {
        val existing = pointHistoryRepository.findOneOrNull(idempotencyKey)
        if (existing != null) {
            throw DuplicateException(EntityType.POINT_HISTORY, "idempotencyKey", idempotencyKey)
        }
    }
}
