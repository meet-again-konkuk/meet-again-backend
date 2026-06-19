package com.konkuk.ma.domain.community.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object PostLikeTable : BaseTable("COMMUNITY_POST_LIKES", "COMMUNITY_POST_LIKE_ID") {
    val postId = long("POST_ID")
    val memberId = long("MEMBER_ID")
}
