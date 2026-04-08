package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.domain.CommentLike
import com.konkuk.ma.domain.community.entity.table.CommentLikeTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.insertAndGetId
import org.springframework.stereotype.Component

@Component
class CommentLikeDao {
    fun save(commentLike: CommentLike): Long {
        return CommentLikeTable.insertAndGetId {
            it[this.commentId] = commentLike.commentId
            it[memberEmail] = commentLike.memberEmail
            it[createdBy] = commentLike.memberEmail
            it[lastModifiedBy] = commentLike.memberEmail
        }.value
    }

    fun delete(commentId: Long, memberEmail: String) {
        CommentLikeTable.deleteWhere {
            (CommentLikeTable.commentId eq commentId) and
                (CommentLikeTable.memberEmail eq memberEmail)
        }
    }
}
