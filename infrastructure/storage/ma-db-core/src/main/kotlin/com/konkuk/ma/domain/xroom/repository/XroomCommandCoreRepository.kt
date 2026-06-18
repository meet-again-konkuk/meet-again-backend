package com.konkuk.ma.domain.xroom.repository

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.xroom.dao.XroomCommandDao
import com.konkuk.ma.domain.xroom.domain.NewXroom
import com.konkuk.ma.domain.xroom.domain.port.XroomCommandRepository
import org.springframework.stereotype.Repository

@Repository
class XroomCommandCoreRepository(
    private val xroomCommandDao: XroomCommandDao,
) : XroomCommandRepository {
    override fun save(newXroom: NewXroom): Long {
        return xroomCommandDao.save(newXroom)
    }

    override fun delete(ownerEmail: Email) {
        xroomCommandDao.delete(ownerEmail.value)
    }
}
