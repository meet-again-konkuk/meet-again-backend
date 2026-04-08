package com.konkuk.ma.domain.community.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object CommentTable : BaseTable("COMMUNITY_COMMENTS", "COMMUNITY_COMMENT_ID") {
    val postId = long("POST_ID")
    val authorEmail = varchar("AUTHOR_EMAIL", 255)
    val content = text("CONTENT")
    val parentCommentId = long("PARENT_COMMENT_ID").nullable()
    val likes = integer("LIKES").clientDefault { 0 }
}
