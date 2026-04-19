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
        pointProductCacheDao.save(products.map { toCached(it) })
    }

    private fun resolvePolicy(cached: CachedPointProduct): DiscountPolicy? {
        val typeStr = cached.discountType ?: return null
        val factory = findFactory(DiscountType.valueOf(typeStr))
        return factory.create(cached)
    }

    private fun toCached(product: PointProductWithDiscount): CachedPointProduct {
        val base = CachedPointProduct.ofProduct(product.pointProduct)
        val policy = product.discountPolicy ?: return base
        return findFactory(policy.type).serialize(policy, base)
    }

    private fun findFactory(type: DiscountType): CachedDiscountPolicyFactory {
        return discountPolicyFactories.find { it.type == type }
            ?: error("등록되지 않은 할인 정책 타입: $type")
    }
}
