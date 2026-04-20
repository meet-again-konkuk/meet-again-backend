package com.konkuk.ma.domain.xroom.dao

import com.konkuk.ma.domain.xroom.domain.block.NewVideo
import com.konkuk.ma.domain.xroom.entity.table.XroomBlockVideoTable
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Component

@Component
class XroomBlockVideoCommandDao {
    fun saveAll(blockId: Long, newVideos: List<NewVideo>): List<Long> {
        if (newVideos.isEmpty()) return emptyList()
        return XroomBlockVideoTable.batchInsert(newVideos) { newVideo ->
            this[XroomBlockVideoTable.blockId] = blockId
            this[XroomBlockVideoTable.videoUrl] = newVideo.videoUrl
            this[XroomBlockVideoTable.orderIndex] = newVideo.orderIndex
        }.map { it[XroomBlockVideoTable.id].value }
    }

    fun replace(videoId: Long, newVideo: NewVideo) {
        XroomBlockVideoTable.update({ XroomBlockVideoTable.id eq videoId }) {
            it[videoUrl] = newVideo.videoUrl
            it[orderIndex] = newVideo.orderIndex
        }
    }
}
