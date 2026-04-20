package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.domain.xroom.entity.XroomBlockPhotoEntity
import com.konkuk.ma.domain.xroom.entity.table.XroomBlockPhotoTable
import org.springframework.stereotype.Component

@Component
class XroomBlockPhotoQueryDao {
    fun exists(blockId: Long): Boolean {
        return XroomBlockPhotoTable
            .activeRows { XroomBlockPhotoTable.blockId eq blockId }
            .limit(1)
            .any()
    }

    fun findOne(photoId: Long): XroomBlockPhotoEntity? {
        return XroomBlockPhotoTable
            .activeRows { XroomBlockPhotoTable.id eq photoId }
            .limit(1)
            .firstOrNull()
            ?.let { XroomBlockPhotoEntity.from(it) }
    }
}
