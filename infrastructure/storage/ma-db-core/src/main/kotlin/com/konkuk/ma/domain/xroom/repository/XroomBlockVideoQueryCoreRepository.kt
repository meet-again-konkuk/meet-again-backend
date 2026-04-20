package com.konkuk.ma.domain.xroom.repository

import com.konkuk.ma.domain.xroom.dao.XroomBlockVideoQueryDao
import com.konkuk.ma.domain.xroom.domain.block.Video
import com.konkuk.ma.domain.xroom.domain.port.XroomBlockVideoQueryRepository
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Repository

@Repository
class XroomBlockVideoQueryCoreRepository(
    private val xroomBlockVideoQueryDao: XroomBlockVideoQueryDao,
) : XroomBlockVideoQueryRepository {
    override fun exists(blockId: Long): Boolean {
        return xroomBlockVideoQueryDao.exists(blockId)
    }

    override fun findOne(videoId: Long): Video {
        val entity = xroomBlockVideoQueryDao.findOne(videoId)
            ?: throw EntityNotFoundException(EntityType.XROOM_BLOCK_VIDEO, videoId.toString())
        return entity.toDomain()
    }
}
