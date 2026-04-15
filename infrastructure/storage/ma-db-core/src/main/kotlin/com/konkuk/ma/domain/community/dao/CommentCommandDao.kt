package com.konkuk.ma.domain.community.dao

import com.konkuk.ma.domain.community.domain.NewComment
import com.konkuk.ma.domain.community.entity.table.CommentTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Component

@Component
class CommentCommandDao {
    fun save(newComment: NewComment): Long {
        return CommentTable.insertAndGetId {
            it[postId] = newComment.postId
            it[authorEmail] = newComment.authorEmail.value
            it[content] = newComment.content
            it[parentCommentId] = newComment.parentCommentId
            it[createdBy] = newComment.authorEmail.value
            it[lastModifiedBy] = newComment.authorEmail.value
        }.value
    }

    fun increaseLikes(commentId: Long): Int {
        CommentTable.update({ CommentTable.id eq commentId }) {
            with(org.jetbrains.exposed.sql.SqlExpressionBuilder) {
                it[likes] = likes + 1
            }
        }
        return findLikeCount(commentId)
    }

    fun decreaseLikes(commentId: Long): Int {
        CommentTable.update({ CommentTable.id eq commentId }) {
            with(org.jetbrains.exposed.sql.SqlExpressionBuilder) {
                it[likes] = likes - 1
            }
        }
        return findLikeCount(commentId)
    }

    fun delete(id: Long) {
        CommentTable.update({ CommentTable.id eq id }) {
            it[deleted] = true
        }
    }

    private fun findLikeCount(commentId: Long): Int {
        return CommentTable
            .select(CommentTable.likes)
            .where { CommentTable.id eq commentId }
            .limit(1)
            .first()[CommentTable.likes]
    }
}
