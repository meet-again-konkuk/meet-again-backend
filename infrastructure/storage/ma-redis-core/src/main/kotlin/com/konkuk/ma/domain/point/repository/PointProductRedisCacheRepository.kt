package com.konkuk.ma.domain.point.repository

import com.konkuk.ma.domain.point.dao.CachedPointProduct
import com.konkuk.ma.domain.point.dao.PointProductCacheDao
import com.konkuk.ma.domain.point.domain.PointProduct
import com.konkuk.ma.domain.point.domain.port.PointProductCacheRepository
import org.springframework.stereotype.Repository

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
        )
    }

    private fun PointProduct.toCached(): CachedPointProduct {
        return CachedPointProduct(
            pointProductId = pointProductId,
            name = name,
            quantity = quantity,
            price = price,
            displayOrder = displayOrder,
        )
    }
}
