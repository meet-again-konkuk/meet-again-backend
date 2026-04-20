package com.konkuk.ma.domain.xroom.repository

import com.konkuk.ma.domain.xroom.dao.XroomBlockQueryDao
import com.konkuk.ma.domain.xroom.domain.block.XroomBlock
import com.konkuk.ma.domain.xroom.domain.port.XroomBlockQueryRepository
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Repository

@Repository
class XroomBlockQueryCoreRepository(
    private val xroomBlockQueryDao: XroomBlockQueryDao,
) : XroomBlockQueryRepository {
    override fun findOne(blockId: Long): XroomBlock {
        val entity = xroomBlockQueryDao.findOne(blockId)
            ?: throw EntityNotFoundException(EntityType.XROOM_BLOCK, blockId.toString())
        return entity.toDomain()
    }
}
