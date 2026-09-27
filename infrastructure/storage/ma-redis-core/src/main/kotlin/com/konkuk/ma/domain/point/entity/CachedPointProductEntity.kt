package com.konkuk.ma.domain.point.entity

import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.domain.PointProduct
import com.konkuk.ma.domain.point.domain.PointProductWithDiscount
import com.konkuk.ma.domain.point.domain.balance.PointQuantity

data class CachedPointProductEntity(
    val pointProductId: Long = 0,
    val name: String = "",
    val quantity: Int = 0,
    val price: Int = 0,
    val displayOrder: Int = 0,
    val discount: CachedDiscount? = null,
) {
    fun toDomain(): PointProductWithDiscount {
        val policy = discount?.toDomain()
        val product = PointProduct(
            pointProductId = pointProductId,
            name = name,
            quantity = PointQuantity(quantity),
            price = Money.wons(price),
            displayOrder = displayOrder,
            discountPolicyId = policy?.discountPolicyId,
        )
        return PointProductWithDiscount(product, policy)
    }

    companion object {
        fun from(product: PointProductWithDiscount): CachedPointProductEntity {
            return CachedPointProductEntity(
                pointProductId = product.pointProduct.pointProductId,
                name = product.pointProduct.name,
                quantity = product.pointProduct.quantity.toInt(),
                price = product.pointProduct.price.toInt(),
                displayOrder = product.pointProduct.displayOrder,
                discount = product.discountPolicy?.let(CachedDiscount::fromPolicy),
            )
        }
    }
}
