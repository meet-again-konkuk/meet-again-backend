package com.konkuk.ma.domain.xroom.entity.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.date

object DdayBlockTable : LongIdTable("XROOM_DDAY_BLOCKS", "XROOM_BLOCK_ID") {
    val anniversaryDate = date("ANNIVERSARY_DATE")
    val label = varchar("LABEL", 100)
}
