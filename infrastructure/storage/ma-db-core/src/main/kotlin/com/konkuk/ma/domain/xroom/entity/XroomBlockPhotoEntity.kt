package com.konkuk.ma.domain.xroom.entity

import com.konkuk.ma.domain.xroom.domain.block.Photo
import com.konkuk.ma.domain.xroom.entity.table.XroomBlockPhotoTable
import org.jetbrains.exposed.sql.ResultRow

class XroomBlockPhotoEntity(
    val id: Long,
    val blockId: Long,
    val photoUrl: String,
    val orderIndex: Int,
) {
    fun toDomain(): Photo {
        return Photo(
            id = id,
            blockId = blockId,
            photoUrl = photoUrl,
            orderIndex = orderIndex,
        )
    }

    companion object {
        fun from(row: ResultRow): XroomBlockPhotoEntity {
            return XroomBlockPhotoEntity(
                id = row[XroomBlockPhotoTable.id].value,
                blockId = row[XroomBlockPhotoTable.blockId],
                photoUrl = row[XroomBlockPhotoTable.photoUrl],
                orderIndex = row[XroomBlockPhotoTable.orderIndex],
            )
        }
    }
}
