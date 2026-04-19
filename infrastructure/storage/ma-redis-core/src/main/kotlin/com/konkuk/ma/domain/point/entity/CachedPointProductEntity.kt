package com.konkuk.ma.domain.point.entity

import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.domain.PointProduct
import com.konkuk.ma.domain.point.domain.PointProductWithDiscount
import com.konkuk.ma.domain.point.domain.discount.AmountDiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.PercentDiscountPolicy

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
            quantity = quantity,
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
                quantity = product.pointProduct.quantity,
                price = product.pointProduct.price.toInt(),
                displayOrder = product.pointProduct.displayOrder,
                discount = toCachedDiscount(product.discountPolicy),
            )
        }

        private fun toCachedDiscount(policy: com.konkuk.ma.domain.point.domain.discount.DiscountPolicy?): CachedDiscount? {
            return when (policy) {
                null -> null
                is AmountDiscountPolicy -> CachedAmountDiscount(
                    startDate = policy.startDate.toString(),
                    endDate = policy.endDate.toString(),
                    discountAmount = policy.discountAmount.toInt(),
                )
                is PercentDiscountPolicy -> CachedPercentDiscount(
                    startDate = policy.startDate.toString(),
                    endDate = policy.endDate.toString(),
                    discountPercent = policy.discountPercent,
                )
            }
        }
    }
}
