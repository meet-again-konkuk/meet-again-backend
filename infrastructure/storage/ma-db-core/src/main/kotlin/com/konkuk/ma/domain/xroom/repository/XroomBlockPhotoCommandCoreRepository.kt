package com.konkuk.ma.domain.xroom.repository

import com.konkuk.ma.domain.xroom.dao.XroomBlockPhotoCommandDao
import com.konkuk.ma.domain.xroom.domain.block.NewPhoto
import com.konkuk.ma.domain.xroom.domain.port.XroomBlockPhotoCommandRepository
import org.springframework.stereotype.Repository

@Repository
class XroomBlockPhotoCommandCoreRepository(
    private val xroomBlockPhotoCommandDao: XroomBlockPhotoCommandDao,
) : XroomBlockPhotoCommandRepository {
    override fun saveAll(blockId: Long, newPhotos: List<NewPhoto>): List<Long> {
        return xroomBlockPhotoCommandDao.saveAll(blockId, newPhotos)
    }

    override fun replace(photoId: Long, newPhoto: NewPhoto) {
        xroomBlockPhotoCommandDao.replace(photoId, newPhoto)
    }
}
