package com.konkuk.ma.domain.xroom.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object XroomTable : BaseTable("XROOMS", "XROOM_ID") {
    val ownerId = long("OWNER_ID")
    val targetInfoId = long("TARGET_INFO_ID")
    val theme = varchar("THEME", 32)
}
