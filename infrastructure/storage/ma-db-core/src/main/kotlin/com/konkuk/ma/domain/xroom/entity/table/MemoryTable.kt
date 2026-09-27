package com.konkuk.ma.domain.xroom.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable
import org.jetbrains.exposed.sql.javatime.date

object MemoryTable : BaseTable("MEMORIES", "MEMORY_ID") {
    val xroomId = long("ROOM_ID").index()
    val title = varchar("TITLE", 200)
    val eventDate = date("EVENT_DATE")
    val eventDatePrecision = varchar("EVENT_DATE_PRECISION", 8)
    val location = varchar("LOCATION", 200).nullable()
    val text = text("TEXT").nullable()
    val letter = text("LETTER").nullable()
}
