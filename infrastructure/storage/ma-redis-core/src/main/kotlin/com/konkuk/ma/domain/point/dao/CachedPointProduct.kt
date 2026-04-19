package com.konkuk.ma.domain.point.dao

import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.domain.PointProduct
import com.konkuk.ma.domain.point.domain.PointProductWithDiscount
import com.konkuk.ma.domain.point.domain.discount.AmountDiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.DiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.PercentDiscountPolicy

data class CachedPointProduct(
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
        fun from(product: PointProductWithDiscount): CachedPointProduct {
            val policy = product.discountPolicy
            return CachedPointProduct(
                pointProductId = product.pointProduct.pointProductId,
                name = product.pointProduct.name,
                quantity = product.pointProduct.quantity,
                price = product.pointProduct.price.toInt(),
                displayOrder = product.pointProduct.displayOrder,
                discountType = policy?.type?.name,
                discountAmount = when (policy) {
                    is AmountDiscountPolicy -> policy.discountAmount.toInt()
                    is PercentDiscountPolicy, null -> null
                },
                discountPercent = when (policy) {
                    is PercentDiscountPolicy -> policy.discountPercent
                    is AmountDiscountPolicy, null -> null
                },
                discountStartDate = policy?.startDate?.toString(),
                discountEndDate = policy?.endDate?.toString(),
            )
        }
    }
}
