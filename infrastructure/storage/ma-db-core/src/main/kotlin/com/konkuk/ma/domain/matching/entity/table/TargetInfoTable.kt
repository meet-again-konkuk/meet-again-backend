package com.konkuk.ma.domain.matching.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object TargetInfoTable : BaseTable("TARGET_INFOS", "TARGET_INFO_ID") {
    val registerId = long("REGISTER_ID").index()
    val name = varchar("NAME", 255)
    val targetGender = varchar("TARGET_GENDER", 32)
    val middleNumber = varchar("MIDDLE_NUMBER", 255).nullable()
    val lastNumber = varchar("LAST_NUMBER", 255).nullable()

    val year = integer("YEAR").nullable()
    val month = integer("MONTH").nullable()
    val day = integer("DAY").nullable()

    val region = varchar("REGION", 255).nullable()
}
