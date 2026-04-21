package com.konkuk.ma.domain.point.domain.port

import com.konkuk.ma.domain.point.domain.transaction.NewPointTransaction
import com.konkuk.ma.domain.point.domain.transaction.PointTransaction

interface PointTransactionRepository {
    fun save(newPointTransaction: NewPointTransaction): Long

    fun findOneOrNull(idempotencyKey: String): PointTransaction?
}
