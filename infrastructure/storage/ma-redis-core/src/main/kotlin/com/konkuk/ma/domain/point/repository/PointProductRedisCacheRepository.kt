package com.konkuk.ma.domain.point.repository

import com.konkuk.ma.domain.point.dao.CachedPointProduct
import com.konkuk.ma.domain.point.dao.PointProductCacheDao
import com.konkuk.ma.domain.point.domain.PointProduct
import com.konkuk.ma.domain.point.domain.discount.AmountDiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.DiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.DiscountType
import com.konkuk.ma.domain.point.domain.discount.PercentDiscountPolicy
import com.konkuk.ma.domain.point.domain.port.PointProductCacheRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class PointProductRedisCacheRepository(
    private val pointProductCacheDao: PointProductCacheDao,
) : PointProductCacheRepository {

    override fun findOrNull(): List<PointProduct>? {
        return pointProductCacheDao.findOrNull()?.map { it.toDomain() }
    }

    override fun save(products: List<PointProduct>) {
        pointProductCacheDao.save(products.map { it.toCached() })
    }

    private fun CachedPointProduct.toDomain(): PointProduct {
        return PointProduct(
            pointProductId = pointProductId,
            name = name,
            quantity = quantity,
            price = price,
            displayOrder = displayOrder,
            discountPolicy = toDiscountPolicy(),
        )
    }

    private fun CachedPointProduct.toDiscountPolicy(): DiscountPolicy? {
        if (discountType == null || discountStartDate == null || discountEndDate == null) return null
        val startDate = LocalDate.parse(discountStartDate)
        val endDate = LocalDate.parse(discountEndDate)
        return when (DiscountType.valueOf(discountType)) {
            DiscountType.AMOUNT -> AmountDiscountPolicy(
                discountPolicyId = 0,
                startDate = startDate,
                endDate = endDate,
                discountAmount = requireNotNull(discountAmount) { "AMOUNT 정책은 discountAmount가 필수입니다" },
            )
            DiscountType.PERCENT -> PercentDiscountPolicy(
                discountPolicyId = 0,
                startDate = startDate,
                endDate = endDate,
                discountPercent = requireNotNull(discountPercent) { "PERCENT 정책은 discountPercent가 필수입니다" },
            )
        }
    }

    private fun PointProduct.toCached(): CachedPointProduct {
        return CachedPointProduct(
            pointProductId = pointProductId,
            name = name,
            quantity = quantity,
            price = price,
            displayOrder = displayOrder,
            discountType = discountPolicy?.type?.name,
            discountAmount = (discountPolicy as? AmountDiscountPolicy)?.discountAmount,
            discountPercent = (discountPolicy as? PercentDiscountPolicy)?.discountPercent,
            discountStartDate = discountPolicy?.startDate?.toString(),
            discountEndDate = discountPolicy?.endDate?.toString(),
        )
    }
}
