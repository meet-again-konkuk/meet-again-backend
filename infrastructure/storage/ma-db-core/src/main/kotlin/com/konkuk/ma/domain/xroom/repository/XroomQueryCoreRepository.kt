package com.konkuk.ma.domain.xroom.repository

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.xroom.dao.XroomQueryDao
import com.konkuk.ma.domain.xroom.domain.Xroom
import com.konkuk.ma.domain.xroom.domain.port.XroomQueryRepository
import org.springframework.stereotype.Repository

@Repository
class XroomQueryCoreRepository(
    private val xroomQueryDao: XroomQueryDao,
) : XroomQueryRepository {
    override fun find(ownerEmail: Email): List<Xroom> {
        return xroomQueryDao.find(ownerEmail.value).map { it.toDomain() }
    }

    override fun exists(targetInfoId: Long): Boolean {
        return xroomQueryDao.exists(targetInfoId)
    }

    override fun exists(targetInfoIds: Set<Long>): Set<Long> {
        return xroomQueryDao.exists(targetInfoIds)
    }
}
