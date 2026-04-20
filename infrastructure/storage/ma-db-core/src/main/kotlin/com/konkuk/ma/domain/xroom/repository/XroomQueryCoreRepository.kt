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
    override fun exists(targetInfoId: Long): Boolean {
        return xroomQueryDao.exists(targetInfoId)
    }

    override fun exists(targetInfoIds: Set<Long>): Set<Long> {
        return xroomQueryDao.exists(targetInfoIds)
    }

    override fun findOne(xroomId: Long): Xroom {
        val entity = xroomQueryDao.findOne(xroomId)
            ?: throw EntityNotFoundException(EntityType.XROOM, xroomId.toString())
        return entity.toDomain()
    }
}
