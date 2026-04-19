package com.konkuk.ma.domain.point.entity

import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.domain.PointProduct
import com.konkuk.ma.domain.point.domain.PointProductWithDiscount
import com.konkuk.ma.domain.point.domain.discount.DiscountPolicy

data class CachedPointProductEntity(
    val pointProductId: Long = 0,
    val name: String = "",
    val quantity: Int = 0,
    val price: Int = 0,
    val displayOrder: Int = 0,
    val discountType: String? = null,
    val discountAmount: Int? = null,
    val discountPercent: Int? = null,
    val discountStartDate: String? = null,
    val discountEndDate: String? = null,
) {
    fun toDomain(discountPolicy: DiscountPolicy?): PointProductWithDiscount {
        val product = PointProduct(
            pointProductId = pointProductId,
            name = name,
            quantity = quantity,
            price = Money.wons(price),
            displayOrder = displayOrder,
            discountPolicyId = discountPolicy?.discountPolicyId,
        )
        return PointProductWithDiscount(product, discountPolicy)
    }

    companion object {
        fun ofProduct(product: PointProduct): CachedPointProductEntity {
            return CachedPointProductEntity(
                pointProductId = product.pointProductId,
                name = product.name,
                quantity = product.quantity,
                price = product.price.toInt(),
                displayOrder = product.displayOrder,
            )
        }
    }
}
