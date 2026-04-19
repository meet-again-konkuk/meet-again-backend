package com.konkuk.ma.domain.point.repository

import com.konkuk.ma.domain.point.dao.CachedDiscountPolicyFactory
import com.konkuk.ma.domain.point.dao.CachedPointProduct
import com.konkuk.ma.domain.point.dao.PointProductCacheDao
import com.konkuk.ma.domain.point.domain.PointProductWithDiscount
import com.konkuk.ma.domain.point.domain.discount.DiscountPolicy
import com.konkuk.ma.domain.point.domain.discount.DiscountType
import com.konkuk.ma.domain.point.domain.port.PointProductCacheRepository
import org.springframework.stereotype.Repository

@Repository
class PointProductRedisCacheRepository(
    private val pointProductCacheDao: PointProductCacheDao,
    private val discountPolicyFactories: List<CachedDiscountPolicyFactory>,
) : PointProductCacheRepository {

    override fun findOrNull(): List<PointProductWithDiscount>? {
        return pointProductCacheDao.findOrNull()?.map { cached ->
            cached.toDomain(resolvePolicy(cached))
        }
    }

    override fun save(products: List<PointProductWithDiscount>) {
        pointProductCacheDao.save(products.map { CachedPointProduct.from(it) })
    }

    private fun resolvePolicy(cached: CachedPointProduct): DiscountPolicy? {
        val typeStr = cached.discountType ?: return null
        val type = DiscountType.valueOf(typeStr)
        val factory = discountPolicyFactories.find { it.type == type }
            ?: error("등록되지 않은 할인 정책 타입: $type")
        return factory.create(cached)
    }
}
