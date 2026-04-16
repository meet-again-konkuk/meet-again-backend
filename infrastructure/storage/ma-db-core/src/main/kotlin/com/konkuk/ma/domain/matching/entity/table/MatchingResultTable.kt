package com.konkuk.ma.domain.matching.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable
import com.konkuk.ma.domain.member.entity.table.MemberTable
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime

object MatchingResultTable : BaseTable("MATCHING_RESULTS", "MATCHING_RESULT_ID") {
    val registerEmail = varchar("REGISTER_EMAIL", 255).references(MemberTable.email)
    val targetInfoId = long("TARGET_INFO_ID").references(TargetInfoTable.id)
    val targetEmail = varchar("TARGET_EMAIL", 255).references(MemberTable.email)
    val middleNumberMatched = bool("MIDDLE_NUMBER_MATCHED")
    val lastNumberMatched = bool("LAST_NUMBER_MATCHED")
    val yearMatched = bool("YEAR_MATCHED")
    val monthMatched = bool("MONTH_MATCHED")
    val dayMatched = bool("DAY_MATCHED")
    val regionMatched = bool("REGION_MATCHED")
    val showingExpiryDate = datetime("SHOWING_EXPIRY_DATE")
    val matchingExpiryDate = date("MATCHING_EXPIRY_DATE")
    val excluded = bool("EXCLUDED").clientDefault { false }
    val claimed = bool("CLAIMED").clientDefault { false }
}
