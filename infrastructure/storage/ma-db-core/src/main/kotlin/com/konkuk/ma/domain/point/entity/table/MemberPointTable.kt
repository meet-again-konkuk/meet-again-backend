package com.konkuk.ma.domain.point.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object MemberPointTable : BaseTable("MEMBER_POINTS", "MEMBER_POINT_ID") {
    val ownerEmail = varchar("OWNER_EMAIL", 255).uniqueIndex()
    val balance = integer("BALANCE")
}
