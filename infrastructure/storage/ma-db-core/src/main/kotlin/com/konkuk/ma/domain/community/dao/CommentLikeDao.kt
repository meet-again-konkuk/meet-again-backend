package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.domain.CommentLike
import com.konkuk.ma.domain.community.entity.CommentLikeEntity
import com.konkuk.ma.domain.community.entity.table.CommentLikeTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Component

@Component
class CommentLikeDao {
    fun findOne(commentId: Long, memberEmail: String): CommentLikeEntity? {
        return CommentLikeTable
            .selectAll()
            .where {
                (CommentLikeTable.commentId eq commentId) and
                    (CommentLikeTable.memberEmail eq memberEmail) and
                    (CommentLikeTable.deleted eq false)
            }
            .map { row -> CommentLikeEntity.from(row) }
            .singleOrNull()
    }

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
