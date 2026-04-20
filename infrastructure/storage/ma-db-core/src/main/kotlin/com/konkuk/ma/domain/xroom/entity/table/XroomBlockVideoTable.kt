package com.konkuk.ma.domain.xroom.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object XroomBlockVideoTable : BaseTable("XROOM_BLOCK_VIDEOS", "XROOM_BLOCK_VIDEO_ID") {
    val blockId = long("BLOCK_ID")
    val videoUrl = varchar("VIDEO_URL", 512)
    val orderIndex = integer("ORDER_INDEX")
}
