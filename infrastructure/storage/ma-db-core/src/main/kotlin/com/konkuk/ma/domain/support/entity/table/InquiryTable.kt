package com.konkuk.ma.domain.support.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object InquiryTable : BaseTable("SUPPORT_INQUIRIES", "SUPPORT_INQUIRY_ID") {
    val authorEmail = varchar("AUTHOR_EMAIL", 255)
    val title = varchar("TITLE", 100)
    val content = varchar("CONTENT", 1500)
}
