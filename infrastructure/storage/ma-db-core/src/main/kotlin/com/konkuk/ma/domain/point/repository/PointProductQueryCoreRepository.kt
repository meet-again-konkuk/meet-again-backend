package com.konkuk.ma.domain.point.repository

import com.konkuk.ma.domain.point.dao.DiscountPolicyQueryDao
import com.konkuk.ma.domain.point.dao.PointProductQueryDao
import com.konkuk.ma.domain.point.domain.PointProduct
import com.konkuk.ma.domain.point.domain.discount.DiscountPolicy
import com.konkuk.ma.domain.point.domain.port.PointProductQueryRepository
import org.springframework.stereotype.Repository

@Repository
class PointProductQueryCoreRepository(
    private val pointProductQueryDao: PointProductQueryDao,
    private val discountPolicyQueryDao: DiscountPolicyQueryDao,
) : PointProductQueryRepository {
    override fun find(): List<PointProduct> {
        val entities = pointProductQueryDao.find()
        val discountPolicyIds = entities.mapNotNull { it.discountPolicyId }.distinct()
        val discountPolicyMap = findDiscountPolicies(discountPolicyIds)

        return entities.map { entity ->
            entity.toDomain(discountPolicyMap[entity.discountPolicyId])
        }
    }

    private fun findDiscountPolicies(ids: List<Long>): Map<Long, DiscountPolicy> {
        if (ids.isEmpty()) return emptyMap()
        return discountPolicyQueryDao.find(ids)
            .map { it.toDomain() }
            .associateBy { it.discountPolicyId }
    }
}
