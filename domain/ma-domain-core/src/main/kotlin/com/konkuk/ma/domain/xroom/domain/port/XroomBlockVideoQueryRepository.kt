package com.konkuk.ma.domain.xroom.domain.port

import com.konkuk.ma.domain.xroom.domain.block.Video

interface XroomBlockVideoQueryRepository {
    fun exists(blockId: Long): Boolean

    fun findOne(videoId: Long): Video
}
