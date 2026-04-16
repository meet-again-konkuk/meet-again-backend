package com.konkuk.ma.domain.point.api.response

import com.konkuk.ma.domain.point.domain.PointProduct
import com.konkuk.ma.domain.point.domain.discount.AmountDiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.PercentDiscountPolicy
import java.time.LocalDate

class PointProductResponse(
    val pointProductId: Long,
    val name: String,
    val quantity: Int,
    val price: Int,
    val discountedPrice: Int?,
    val discountType: String?,
    val isDiscountActive: Boolean,
) {
    companion object {
        fun from(pointProduct: PointProduct): PointProductResponse {
            val now = LocalDate.now()
            val policy = pointProduct.discountPolicy
            val isActive = policy?.isActive(now) ?: false
            return PointProductResponse(
                pointProductId = pointProduct.pointProductId,
                name = pointProduct.name,
                quantity = pointProduct.quantity,
                price = pointProduct.price,
                discountedPrice = if (isActive) pointProduct.discountedPrice(now) else null,
                discountType = policy?.let {
                    when (it) {
                        is AmountDiscountPolicy -> "AMOUNT"
                        is PercentDiscountPolicy -> "PERCENT"
                    }
                },
                isDiscountActive = isActive,
            )
        }
    }
}
