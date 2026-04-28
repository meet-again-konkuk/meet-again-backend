package com.konkuk.ma.domain.point.repository

import com.konkuk.ma.domain.point.dao.DiscountPolicyQueryDao
import com.konkuk.ma.domain.point.domain.discount.DiscountPolicy
import com.konkuk.ma.domain.point.domain.port.DiscountPolicyQueryRepository
import org.springframework.stereotype.Repository

@Repository
class DiscountPolicyCoreRepository(
    private val discountPolicyQueryDao: DiscountPolicyQueryDao,
) : DiscountPolicyQueryRepository {
    override fun find(ids: Set<Long>): List<DiscountPolicy> {
        return discountPolicyQueryDao.find(ids).map { it.toDomain() }
    }

    override fun findOneOrNull(id: Long): DiscountPolicy? {
        return discountPolicyQueryDao.findOneOrNull(id)?.toDomain()
    }
}
