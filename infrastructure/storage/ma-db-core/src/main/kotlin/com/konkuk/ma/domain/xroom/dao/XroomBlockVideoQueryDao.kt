package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.domain.xroom.entity.XroomBlockVideoEntity
import com.konkuk.ma.domain.xroom.entity.table.XroomBlockVideoTable
import org.springframework.stereotype.Component

@Component
class XroomBlockVideoQueryDao {
    fun exists(blockId: Long): Boolean {
        return XroomBlockVideoTable
            .activeRows { XroomBlockVideoTable.blockId eq blockId }
            .limit(1)
            .any()
    }

    fun findOne(videoId: Long): XroomBlockVideoEntity? {
        return XroomBlockVideoTable
            .activeRows { XroomBlockVideoTable.id eq videoId }
            .limit(1)
            .firstOrNull()
            ?.let { XroomBlockVideoEntity.from(it) }
    }
}
