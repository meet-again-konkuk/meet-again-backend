package com.konkuk.ma.domain.matching.repository

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.matching.dao.TargetInfoQueryDao
import com.konkuk.ma.domain.matching.domain.TargetInfo
import com.konkuk.ma.domain.matching.domain.port.TargetInfoQueryRepository
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Repository

@Repository
class TargetInfoQueryCoreRepository(
    private val targetInfoQueryDao: TargetInfoQueryDao,
) : TargetInfoQueryRepository {
    override fun findOne(id: Long): TargetInfo {
        return targetInfoQueryDao.findOne(id)?.toDomain()
            ?: throw EntityNotFoundException(EntityType.TARGET_INFO, id.toString())
    }

    override fun find(email: Email): List<TargetInfo> {
        return targetInfoQueryDao.find(email.value).map { it.toDomain() }
    }

    override fun findNoOffset(cursorId: Long?, size: Int): List<TargetInfo> {
        return targetInfoQueryDao.findNoOffset(cursorId, size)
            .map { it.toDomain() }
    }
}
