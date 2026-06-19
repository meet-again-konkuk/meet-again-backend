package com.konkuk.ma.domain.community.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object PostTable : BaseTable("COMMUNITY_POSTS", "COMMUNITY_POST_ID") {
    val authorId = long("AUTHOR_ID")
    val category = varchar("CATEGORY", 32)
    val title = varchar("TITLE", 100)
    val content = text("CONTENT")
}
