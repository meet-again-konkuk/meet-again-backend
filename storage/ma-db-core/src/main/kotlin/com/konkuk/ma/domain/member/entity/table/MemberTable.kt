package com.konkuk.ma.domain.member.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object MemberTable : BaseTable("MEMBERS", "MEMBER_ID") {
    val email = varchar("EMAIL", 255).uniqueIndex()
    val password = varchar("PASSWORD", 255)
    val nickname = varchar("NICKNAME", 255)
    val phoneNumber = varchar("PHONE_NUMBER", 255)

    val profileImageUrl = varchar("PROFILE_IMAGE_URL", 255).nullable()
}
