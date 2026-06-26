package com.konkuk.ma.domain.xroom.repository

import com.konkuk.ma.domain.xroom.dao.XroomQueryDao
import com.konkuk.ma.domain.xroom.domain.Xroom
import com.konkuk.ma.domain.xroom.domain.port.XroomQueryRepository
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Repository

@Repository
class XroomQueryCoreRepository(
    private val xroomQueryDao: XroomQueryDao,
) : XroomQueryRepository {
    override fun find(ownerId: Long): List<Xroom> {
        return xroomQueryDao.find(ownerId).map { it.toDomain() }
    }

    override fun findOne(xroomId: Long): Xroom {
        return xroomQueryDao.findOne(xroomId)?.toDomain()
            ?: throw EntityNotFoundException(EntityType.XROOM, xroomId.toString())
    }

    override fun exists(targetInfoId: Long): Boolean {
        return xroomQueryDao.exists(targetInfoId)
    }

    override fun exists(targetInfoIds: Set<Long>): Set<Long> {
        return xroomQueryDao.exists(targetInfoIds)
    }
}
