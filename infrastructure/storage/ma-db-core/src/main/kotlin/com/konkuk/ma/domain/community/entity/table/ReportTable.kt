package com.konkuk.ma.domain.community.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object ReportTable : BaseTable("COMMUNITY_REPORTS", "COMMUNITY_REPORT_ID") {
    val reporterId = long("REPORTER_ID")
    val targetType = varchar("TARGET_TYPE", 20)
    val targetId = long("TARGET_ID")
    val targetAuthorId = long("TARGET_AUTHOR_ID")
    val targetTitle = varchar("TARGET_TITLE", 255).nullable()
    val targetContent = text("TARGET_CONTENT")
    val reason = varchar("REASON", 20)
    val detail = varchar("DETAIL", 500).nullable()
    val status = varchar("STATUS", 20)

    init {
        index("IDX_COMMUNITY_REPORTS_REPORTER_TARGET", false, reporterId, targetType, targetId)
        index("IDX_COMMUNITY_REPORTS_TARGET", false, targetType, targetId)
    }
}
