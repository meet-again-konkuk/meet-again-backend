package com.konkuk.ma.domain.member.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime

object MemberTable : BaseTable("MEMBERS", "MEMBER_ID") {
    val email = varchar("EMAIL", 255).uniqueIndex()
    val password = varchar("PASSWORD", 255)
    val nickname = varchar("NICKNAME", 255)
    val gender = varchar("GENDER", 32)
    val phoneNumber = varchar("PHONE_NUMBER", 255).index()
    val name = varchar("NAME", 255)
    val birthDate = date("BIRTH_DATE")
    val region = varchar("REGION", 255)
    val highSchool = varchar("HIGH_SCHOOL", 255).nullable()
    val university = varchar("UNIVERSITY", 255).nullable()

    val profileImageUrl = varchar("PROFILE_IMAGE_URL", 255).nullable()

    val withdrawalRequestedAt = datetime("WITHDRAWAL_REQUESTED_AT").nullable().index()
}
