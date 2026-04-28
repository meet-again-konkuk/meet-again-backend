package com.konkuk.ma.domain.point.domain.port

import com.konkuk.ma.domain.point.domain.history.NewPointHistory
import com.konkuk.ma.domain.point.domain.history.PointHistory

interface PointHistoryRepository {
    fun save(newPointHistory: NewPointHistory): Long

    fun findOneOrNull(idempotencyKey: String): PointHistory?
}
