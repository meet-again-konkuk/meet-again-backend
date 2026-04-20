package com.konkuk.ma.domain.xroom.entity.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.date

object PhotoBlockTable : LongIdTable("XROOM_PHOTO_BLOCKS", "XROOM_BLOCK_ID") {
    val photoDate = date("PHOTO_DATE").nullable()
}
