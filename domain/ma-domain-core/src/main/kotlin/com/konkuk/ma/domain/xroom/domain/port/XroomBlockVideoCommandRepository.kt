package com.konkuk.ma.domain.xroom.domain.port

import com.konkuk.ma.domain.xroom.domain.block.NewVideo

interface XroomBlockVideoCommandRepository {
    fun saveAll(blockId: Long, newVideos: List<NewVideo>): List<Long>

    fun replace(videoId: Long, newVideo: NewVideo)
}
