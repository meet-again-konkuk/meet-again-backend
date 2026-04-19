package com.konkuk.ma.domain.point.api.response

import com.konkuk.ma.domain.point.domain.PointProductWithDiscount
import java.math.RoundingMode

class PointProductResponse(
    val pointProductId: Long,
    val name: String,
    val quantity: Int,
    val price: Int,
    val discountedPrice: Int,
    val discountRate: Int,
    val discountType: String?,
    val isDiscountActive: Boolean,
) {
    companion object {
        fun from(product: PointProductWithDiscount): PointProductResponse {
            return PointProductResponse(
                pointProductId = product.pointProduct.pointProductId,
                name = product.pointProduct.name,
                quantity = product.pointProduct.quantity,
                price = product.pointProduct.price.toInt(),
                discountedPrice = product.discountedPrice().toInt(),
                discountRate = product.discountRate().setScale(0, RoundingMode.HALF_UP).toInt(),
                discountType = product.discountType()?.name,
                isDiscountActive = product.isDiscountActive(),
            )
        }
    }
}
