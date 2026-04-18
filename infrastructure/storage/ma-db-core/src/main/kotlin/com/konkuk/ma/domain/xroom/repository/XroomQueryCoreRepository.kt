package com.konkuk.ma.domain.xroom.repository

import com.konkuk.ma.domain.xroom.dao.XroomQueryDao
import com.konkuk.ma.domain.xroom.domain.port.XroomQueryRepository
import org.springframework.stereotype.Repository

@Repository
class XroomQueryCoreRepository(
    private val xroomQueryDao: XroomQueryDao,
) : XroomQueryRepository {
    override fun exists(targetInfoId: Long): Boolean {
        return xroomQueryDao.exists(targetInfoId)
    }
}
