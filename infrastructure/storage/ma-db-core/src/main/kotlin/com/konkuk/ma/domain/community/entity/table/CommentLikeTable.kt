package com.konkuk.ma.domain.community.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object CommentLikeTable : BaseTable("COMMUNITY_COMMENT_LIKES", "COMMUNITY_COMMENT_LIKE_ID") {
    val commentId = long("COMMENT_ID")
    val memberId = long("MEMBER_ID")

    init {
        uniqueIndex(commentId, memberId)
    }
}
