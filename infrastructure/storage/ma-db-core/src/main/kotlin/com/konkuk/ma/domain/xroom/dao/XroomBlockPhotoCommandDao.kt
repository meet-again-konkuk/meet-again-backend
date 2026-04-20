package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.domain.xroom.domain.block.NewPhoto
import com.konkuk.ma.domain.xroom.entity.table.XroomBlockPhotoTable
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Component

@Component
class XroomBlockPhotoCommandDao {
    fun saveAll(blockId: Long, newPhotos: List<NewPhoto>): List<Long> {
        if (newPhotos.isEmpty()) return emptyList()
        return XroomBlockPhotoTable.batchInsert(newPhotos) { newPhoto ->
            this[XroomBlockPhotoTable.blockId] = blockId
            this[XroomBlockPhotoTable.photoUrl] = newPhoto.photoUrl
            this[XroomBlockPhotoTable.orderIndex] = newPhoto.orderIndex
        }.map { it[XroomBlockPhotoTable.id].value }
    }

    fun replace(photoId: Long, newPhoto: NewPhoto) {
        XroomBlockPhotoTable.update({ XroomBlockPhotoTable.id eq photoId }) {
            it[photoUrl] = newPhoto.photoUrl
            it[orderIndex] = newPhoto.orderIndex
        }
    }
}
