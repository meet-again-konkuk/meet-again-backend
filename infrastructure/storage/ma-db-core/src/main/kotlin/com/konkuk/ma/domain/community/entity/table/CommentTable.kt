package com.konkuk.ma.domain.community.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object CommentTable : BaseTable("COMMUNITY_COMMENTS", "COMMUNITY_COMMENT_ID") {
    val postId = long("POST_ID").index()
    val authorId = long("AUTHOR_ID")
    val content = text("CONTENT")
    val parentCommentId = long("PARENT_COMMENT_ID").nullable().index()
}
