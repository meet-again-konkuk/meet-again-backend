package com.konkuk.ma.domain.matching.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable
import com.konkuk.ma.domain.member.entity.table.MemberTable

object TargetInfoTable : BaseTable("TARGET_INFOS", "TARGET_INFO_ID") {
    val registerEmail = varchar("REGISTER_EMAIL", 255).references(MemberTable.email)
    val name = varchar("NAME", 255)
    val targetGender = varchar("TARGET_GENDER", 32)
    val middleNumber = varchar("MIDDLE_NUMBER", 255).nullable()
    val lastNumber = varchar("LAST_NUMBER", 255).nullable()
    
    val year = integer("YEAR").nullable()
    val month = integer("MONTH").nullable()
    val day = integer("DAY").nullable()
    
    val region = varchar("REGION", 255).nullable()
}
