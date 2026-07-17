package com.konkuk.ma.domain.community.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object CommentNotificationSettingTable :
    BaseTable("COMMUNITY_COMMENT_NOTIFICATION_SETTINGS", "COMMENT_NOTIFICATION_SETTING_ID") {
    val memberId = long("MEMBER_ID")
    val postId = long("POST_ID")

    init {
        uniqueIndex(memberId, postId)
    }
}
