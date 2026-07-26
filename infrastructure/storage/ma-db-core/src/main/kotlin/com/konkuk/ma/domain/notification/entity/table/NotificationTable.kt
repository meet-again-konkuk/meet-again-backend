package com.konkuk.ma.domain.notification.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object NotificationTable : BaseTable("NOTIFICATIONS", "NOTIFICATION_ID") {
    val recipientId = long("RECIPIENT_ID").index()
    val type = varchar("TYPE", 32)
    val actorId = long("ACTOR_ID")
    val postId = long("POST_ID")
    val commentId = long("COMMENT_ID")
    val read = bool("IS_READ").clientDefault { false }
}
