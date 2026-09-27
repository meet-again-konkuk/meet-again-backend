package com.konkuk.ma.domain.xroom.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object XroomTable : BaseTable("XROOMS", "XROOM_ID") {
    val ownerId = long("OWNER_ID").index()
    val targetInfoId = long("TARGET_INFO_ID").index()
    val template = varchar("TEMPLATE", 32)
    val title = varchar("TITLE", 100)
    val finalMessage = varchar("FINAL_MESSAGE", 1000).nullable()
}
