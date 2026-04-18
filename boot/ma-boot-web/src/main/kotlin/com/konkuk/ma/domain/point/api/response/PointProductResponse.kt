package com.konkuk.ma.domain.point.api.response

import com.konkuk.ma.domain.point.domain.PointProductWithDiscount
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
        fun from(product: PointProductWithDiscount): PointProductResponse {
            val now = LocalDate.now()
            return PointProductResponse(
                pointProductId = product.pointProduct.pointProductId,
                name = product.pointProduct.name,
                quantity = product.pointProduct.quantity,
                price = product.pointProduct.price,
                discountedPrice = product.discountedPriceOrNull(now),
                discountType = product.discountType()?.name,
                isDiscountActive = product.isDiscountActive(now),
            )
        }
    }
}
