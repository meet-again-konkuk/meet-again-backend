package com.konkuk.ma.domain.point.repository

import com.konkuk.ma.domain.point.dao.PointProductQueryDao
import com.konkuk.ma.domain.point.domain.PointProduct
import com.konkuk.ma.domain.point.domain.port.PointProductQueryRepository
import org.springframework.stereotype.Repository

@Repository
class PointProductQueryCoreRepository(
    private val pointProductQueryDao: PointProductQueryDao,
) : PointProductQueryRepository {
    override fun find(): List<PointProduct> {
        return pointProductQueryDao.find().map { it.toDomain() }
    }
}
