package com.konkuk.ma.domain.xroom.domain.port

import com.konkuk.ma.domain.xroom.domain.block.XroomBlock

interface XroomBlockQueryRepository {
    fun findOne(blockId: Long): XroomBlock
}
