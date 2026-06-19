package com.konkuk.ma.domain.support.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object InquiryTable : BaseTable("INQUIRIES", "INQUIRY_ID") {
    val authorId = long("AUTHOR_ID")
    val title = varchar("TITLE", 100)
    val content = varchar("CONTENT", 1500)
}
