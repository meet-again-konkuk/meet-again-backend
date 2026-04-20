package com.konkuk.ma.domain.xroom.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object XroomBlockPhotoTable : BaseTable("XROOM_BLOCK_PHOTOS", "XROOM_BLOCK_PHOTO_ID") {
    val blockId = long("BLOCK_ID")
    val photoUrl = varchar("PHOTO_URL", 512)
    val orderIndex = integer("ORDER_INDEX")
}
