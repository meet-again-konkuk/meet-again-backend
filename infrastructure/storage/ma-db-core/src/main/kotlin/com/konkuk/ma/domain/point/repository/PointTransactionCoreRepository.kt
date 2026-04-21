package com.konkuk.ma.domain.point.repository

import com.konkuk.ma.domain.point.dao.PointTransactionDao
import com.konkuk.ma.domain.point.domain.port.PointTransactionRepository
import com.konkuk.ma.domain.point.domain.transaction.NewPointTransaction
import com.konkuk.ma.domain.point.domain.transaction.PointTransaction
import com.konkuk.ma.domain.point.entity.PointTransactionEntity
import org.springframework.stereotype.Repository

@Repository
class PointTransactionCoreRepository(
    private val pointTransactionDao: PointTransactionDao,
) : PointTransactionRepository {
    override fun save(newPointTransaction: NewPointTransaction): Long {
        return pointTransactionDao.save(PointTransactionEntity.from(newPointTransaction))
    }

    override fun findOneOrNull(idempotencyKey: String): PointTransaction? {
        return pointTransactionDao.findOneOrNull(idempotencyKey)?.toDomain()
    }
}
