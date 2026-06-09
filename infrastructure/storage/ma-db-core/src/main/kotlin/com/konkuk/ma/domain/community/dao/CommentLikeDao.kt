package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.entity.table.CommentLikeTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.stereotype.Component

@Component
class CommentLikeDao {
    fun save(commentId: Long, memberEmail: String): Long {
        return CommentLikeTable.insertAndGetId {
            it[CommentLikeTable.commentId] = commentId
            it[CommentLikeTable.memberEmail] = memberEmail
            it[createdBy] = memberEmail
            it[lastModifiedBy] = memberEmail
        }.value
    }

    fun delete(commentId: Long, memberEmail: String) {
        CommentLikeTable.deleteWhere {
            (CommentLikeTable.commentId eq commentId) and
                (CommentLikeTable.memberEmail eq memberEmail)
        }
    }

    fun deleteByMember(memberEmail: String) {
        CommentLikeTable.deleteWhere { CommentLikeTable.memberEmail eq memberEmail }
    }
}
