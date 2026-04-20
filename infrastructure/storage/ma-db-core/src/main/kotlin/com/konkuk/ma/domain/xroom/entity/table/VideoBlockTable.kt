package com.konkuk.ma.domain.xroom.entity.table

import org.jetbrains.exposed.dao.id.LongIdTable

object VideoBlockTable : LongIdTable("XROOM_VIDEO_BLOCKS", "XROOM_BLOCK_ID") {
    val description = varchar("DESCRIPTION", 500).nullable()
}
