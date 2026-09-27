package com.konkuk.ma.domain.point.repository

import com.konkuk.ma.domain.point.dao.PointProductQueryDao
import com.konkuk.ma.domain.point.domain.PointProduct
import com.konkuk.ma.domain.point.domain.port.PointProductQueryRepository
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Repository

@Repository
class PointProductQueryCoreRepository(
    private val pointProductQueryDao: PointProductQueryDao,
) : PointProductQueryRepository {
    override fun find(): List<PointProduct> {
        return pointProductQueryDao.find().map { it.toDomain() }
    }

    override fun findOne(id: Long): PointProduct {
        val entity = pointProductQueryDao.findOneOrNull(id)
            ?: throw EntityNotFoundException(EntityType.POINT_PRODUCT, id.toString())
        return entity.toDomain()
    }
}
