package com.konkuk.ma.domain.point.repository

import com.konkuk.ma.domain.point.dao.CachedPointProduct
import com.konkuk.ma.domain.point.dao.PointProductCacheDao
import com.konkuk.ma.domain.point.domain.PointProductWithDiscount
import com.konkuk.ma.domain.point.domain.port.PointProductCacheRepository
import org.springframework.stereotype.Repository

@Repository
class PointProductRedisCacheRepository(
    private val pointProductCacheDao: PointProductCacheDao,
) : PointProductCacheRepository {

    override fun findOrNull(): List<PointProductWithDiscount>? {
        return pointProductCacheDao.findOrNull()?.map { it.toDomain() }
    }

    override fun save(products: List<PointProductWithDiscount>) {
        pointProductCacheDao.save(products.map { CachedPointProduct.from(it) })
    }
}
