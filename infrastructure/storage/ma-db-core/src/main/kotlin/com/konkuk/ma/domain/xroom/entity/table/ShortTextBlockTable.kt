package com.konkuk.ma.domain.xroom.entity.table

import org.jetbrains.exposed.dao.id.LongIdTable

object ShortTextBlockTable : LongIdTable("XROOM_SHORT_TEXT_BLOCKS", "XROOM_BLOCK_ID") {
    val content = varchar("CONTENT", 500)
}
