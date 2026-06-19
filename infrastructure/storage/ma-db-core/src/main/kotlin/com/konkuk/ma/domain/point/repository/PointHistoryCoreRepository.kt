package com.konkuk.ma.domain.point.repository

import com.konkuk.ma.domain.point.dao.PointHistoryDao
import com.konkuk.ma.domain.point.domain.history.NewPointHistory
import com.konkuk.ma.domain.point.domain.history.PointHistory
import com.konkuk.ma.domain.point.domain.port.PointHistoryRepository
import com.konkuk.ma.domain.point.entity.PointHistoryEntity
import org.springframework.stereotype.Repository

@Repository
class PointHistoryCoreRepository(
    private val pointHistoryDao: PointHistoryDao,
) : PointHistoryRepository {
    override fun save(newPointHistory: NewPointHistory): Long {
        return pointHistoryDao.save(PointHistoryEntity.from(newPointHistory))
    }

    override fun findOneOrNull(idempotencyKey: String): PointHistory? {
        return pointHistoryDao.findOneOrNull(idempotencyKey)?.toDomain()
    }

    override fun find(ownerId: Long): List<PointHistory> {
        return pointHistoryDao.find(ownerId).map { it.toDomain() }
    }
}
