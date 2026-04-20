package com.konkuk.ma.domain.xroom.repository

import com.konkuk.ma.domain.xroom.dao.XroomBlockCommandDao
import com.konkuk.ma.domain.xroom.domain.block.NewXroomBlock
import com.konkuk.ma.domain.xroom.domain.port.XroomBlockCommandRepository
import org.springframework.stereotype.Repository

@Repository
class XroomBlockCommandCoreRepository(
    private val xroomBlockCommandDao: XroomBlockCommandDao,
) : XroomBlockCommandRepository {
    override fun save(newBlock: NewXroomBlock): Long {
        return xroomBlockCommandDao.save(newBlock)
    }
}
