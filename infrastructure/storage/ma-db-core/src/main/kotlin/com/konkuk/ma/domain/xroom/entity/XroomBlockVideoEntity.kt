package com.konkuk.ma.domain.xroom.entity

import com.konkuk.ma.domain.xroom.domain.block.Video
import com.konkuk.ma.domain.xroom.entity.table.XroomBlockVideoTable
import org.jetbrains.exposed.sql.ResultRow

class XroomBlockVideoEntity(
    val id: Long,
    val blockId: Long,
    val videoUrl: String,
    val orderIndex: Int,
) {
    fun toDomain(): Video {
        return Video(
            id = id,
            blockId = blockId,
            videoUrl = videoUrl,
            orderIndex = orderIndex,
        )
    }

    companion object {
        fun from(row: ResultRow): XroomBlockVideoEntity {
            return XroomBlockVideoEntity(
                id = row[XroomBlockVideoTable.id].value,
                blockId = row[XroomBlockVideoTable.blockId],
                videoUrl = row[XroomBlockVideoTable.videoUrl],
                orderIndex = row[XroomBlockVideoTable.orderIndex],
            )
        }
    }
}
