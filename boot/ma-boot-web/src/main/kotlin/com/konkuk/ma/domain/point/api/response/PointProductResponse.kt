package com.konkuk.ma.domain.point.api.response

import com.konkuk.ma.domain.point.domain.PointProduct
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
            return PointProductResponse(
                pointProductId = pointProduct.pointProductId,
                name = pointProduct.name,
                quantity = pointProduct.quantity,
                price = pointProduct.price,
                discountedPrice = pointProduct.discountedPriceOrNull(now),
                discountType = pointProduct.discountType()?.name,
                isDiscountActive = pointProduct.isDiscountActive(now),
            )
        }
    }
}
