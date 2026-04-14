package com.konkuk.ma.domain.matching.repository

import com.konkuk.ma.domain.matching.dao.TargetInfoQueryDao
import com.konkuk.ma.domain.matching.domain.TargetInfo
import com.konkuk.ma.domain.matching.domain.port.TargetInfoQueryRepository
import org.springframework.stereotype.Repository

@Repository
class TargetInfoQueryCoreRepository(
    private val targetInfoQueryDao: TargetInfoQueryDao

    ) : TargetInfoQueryRepository {
    override fun find(email: String): List<TargetInfo> {
        return targetInfoQueryDao.find(email).map { it.toDomain() }
    }

    override fun findNoOffset(cursorId: Long?, size: Int): List<TargetInfo> {
        return targetInfoQueryDao.findNoOffset(cursorId, size)
            .map { it.toDomain() }
    }
}
