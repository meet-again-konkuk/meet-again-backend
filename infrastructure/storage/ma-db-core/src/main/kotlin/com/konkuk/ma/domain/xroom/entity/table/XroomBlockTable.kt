package com.konkuk.ma.domain.xroom.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object XroomBlockTable : BaseTable("XROOM_BLOCKS", "XROOM_BLOCK_ID") {
    val xroomId = long("XROOM_ID")
    val blockType = varchar("BLOCK_TYPE", 20)
    val item = varchar("ITEM", 40)
    val positionX = ubyte("POSITION_X")
    val positionY = ubyte("POSITION_Y")
    val rotation = integer("ROTATION")
}
