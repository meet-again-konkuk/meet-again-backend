package com.konkuk.ma.domain.xroom.repository

import com.konkuk.ma.domain.xroom.dao.XroomBlockVideoCommandDao
import com.konkuk.ma.domain.xroom.domain.block.NewVideo
import com.konkuk.ma.domain.xroom.domain.port.XroomBlockVideoCommandRepository
import org.springframework.stereotype.Repository

@Repository
class XroomBlockVideoCommandCoreRepository(
    private val xroomBlockVideoCommandDao: XroomBlockVideoCommandDao,
) : XroomBlockVideoCommandRepository {
    override fun saveAll(blockId: Long, newVideos: List<NewVideo>): List<Long> {
        return xroomBlockVideoCommandDao.saveAll(blockId, newVideos)
    }

    override fun replace(videoId: Long, newVideo: NewVideo) {
        xroomBlockVideoCommandDao.replace(videoId, newVideo)
    }
}
