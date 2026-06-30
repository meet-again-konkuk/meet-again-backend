package com.konkuk.ma.domain.xroom.repository

import com.konkuk.ma.domain.xroom.dao.MediaCommandDao
import com.konkuk.ma.domain.xroom.domain.media.NewMedia
import com.konkuk.ma.domain.xroom.domain.port.MediaCommandRepository
import org.springframework.stereotype.Repository

@Repository
class MediaCommandCoreRepository(
    private val mediaCommandDao: MediaCommandDao,
) : MediaCommandRepository {
    override fun save(newMedia: NewMedia): Long {
        return mediaCommandDao.save(newMedia)
    }

    override fun softDeleteByMemory(memoryId: Long, memberId: Long) {
        mediaCommandDao.softDeleteByMemory(memoryId, memberId)
    }
}
