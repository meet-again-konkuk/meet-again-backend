package com.konkuk.ma.domain.xroom.entity.table

import org.jetbrains.exposed.dao.id.LongIdTable

object LongTextBlockTable : LongIdTable("XROOM_LONG_TEXT_BLOCKS", "XROOM_BLOCK_ID") {
    val content = text("CONTENT")
}
