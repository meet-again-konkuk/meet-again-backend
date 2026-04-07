package com.konkuk.ma.domain.community.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object PostTable : BaseTable("COMMUNITY_POSTS", "COMMUNITY_POST_ID") {
    val authorEmail = varchar("AUTHOR_EMAIL", 255)
    val category = varchar("CATEGORY", 32)
    val title = varchar("TITLE", 100)
    val content = text("CONTENT")
    val likes = integer("LIKES").clientDefault { 0 }
    val comments = integer("COMMENTS").clientDefault { 0 }
}
