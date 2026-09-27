package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.entity.table.CommentNotificationSettingTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.intLiteral
import org.springframework.stereotype.Component

@Component
class CommentNotificationSettingDao {
    fun isOptedOut(memberId: Long, postId: Long): Boolean {
        return CommentNotificationSettingTable
            .select(intLiteral(1))
            .where {
                (CommentNotificationSettingTable.memberId eq memberId) and
                    (CommentNotificationSettingTable.postId eq postId)
            }
            .limit(1)
            .any()
    }

    fun optOut(memberId: Long, postId: Long) {
        CommentNotificationSettingTable.insert {
            it[CommentNotificationSettingTable.memberId] = memberId
            it[CommentNotificationSettingTable.postId] = postId
        }
    }

    fun optIn(memberId: Long, postId: Long) {
        CommentNotificationSettingTable.deleteWhere {
            (CommentNotificationSettingTable.memberId eq memberId) and
                (CommentNotificationSettingTable.postId eq postId)
        }
    }

    fun deleteByMember(memberId: Long) {
        CommentNotificationSettingTable.deleteWhere { CommentNotificationSettingTable.memberId eq memberId }
    }
}
