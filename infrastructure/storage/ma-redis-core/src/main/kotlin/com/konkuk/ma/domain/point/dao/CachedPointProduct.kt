package com.konkuk.ma.domain.point.dao

import com.konkuk.ma.domain.common.domain.Money
import com.konkuk.ma.domain.point.domain.PointProduct
import com.konkuk.ma.domain.point.domain.PointProductWithDiscount
import com.konkuk.ma.domain.point.domain.discount.AmountDiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.DiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.DiscountType
import com.konkuk.ma.domain.point.domain.discount.PercentDiscountPolicy
import java.time.LocalDate

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
    fun toDomain(): PointProductWithDiscount {
        val policy = toDiscountPolicy()
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

    private fun toDiscountPolicy(): DiscountPolicy? {
        if (discountType == null || discountStartDate == null || discountEndDate == null) return null
        val startDate = LocalDate.parse(discountStartDate)
        val endDate = LocalDate.parse(discountEndDate)
        return when (DiscountType.valueOf(discountType)) {
            DiscountType.AMOUNT -> AmountDiscountPolicy(
                discountPolicyId = 0,
                startDate = startDate,
                endDate = endDate,
                discountAmount = Money.wons(
                    requireNotNull(discountAmount) { "AMOUNT 정책은 discountAmount가 필수입니다" }
                ),
            )
            DiscountType.PERCENT -> PercentDiscountPolicy(
                discountPolicyId = 0,
                startDate = startDate,
                endDate = endDate,
                discountPercent = requireNotNull(discountPercent) { "PERCENT 정책은 discountPercent가 필수입니다" },
            )
        }
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
                discountAmount = (policy as? AmountDiscountPolicy)?.discountAmount?.toInt(),
                discountPercent = (policy as? PercentDiscountPolicy)?.discountPercent,
                discountStartDate = policy?.startDate?.toString(),
                discountEndDate = policy?.endDate?.toString(),
            )
        }
    }
}
