package com.konkuk.ma.domain.point.api.response

import com.konkuk.ma.domain.point.domain.PointProduct

class PointProductResponse(
    val pointProductId: Long,
    val name: String,
    val quantity: Int,
    val price: Int,
) {
    companion object {
        fun from(pointProduct: PointProduct): PointProductResponse {
            return PointProductResponse(
                pointProductId = pointProduct.pointProductId,
                name = pointProduct.name,
                quantity = pointProduct.quantity,
                price = pointProduct.price,
            )
        }
    }
}
