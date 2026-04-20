package com.konkuk.ma.domain.xroom.repository

import com.konkuk.ma.domain.xroom.dao.XroomBlockPhotoQueryDao
import com.konkuk.ma.domain.xroom.domain.block.Photo
import com.konkuk.ma.domain.xroom.domain.port.XroomBlockPhotoQueryRepository
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Repository

@Repository
class XroomBlockPhotoQueryCoreRepository(
    private val xroomBlockPhotoQueryDao: XroomBlockPhotoQueryDao,
) : XroomBlockPhotoQueryRepository {
    override fun exists(blockId: Long): Boolean {
        return xroomBlockPhotoQueryDao.exists(blockId)
    }

    override fun findOne(photoId: Long): Photo {
        val entity = xroomBlockPhotoQueryDao.findOne(photoId)
            ?: throw EntityNotFoundException(EntityType.XROOM_BLOCK_PHOTO, photoId.toString())
        return entity.toDomain()
    }
}
