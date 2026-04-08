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
            it[authorEmail] = newComment.authorEmail
            it[content] = newComment.content
            it[parentCommentId] = newComment.parentCommentId
            it[createdBy] = newComment.authorEmail
            it[lastModifiedBy] = newComment.authorEmail
        }.value
    }

    fun increaseLikes(commentId: Long) {
        CommentTable.update({ CommentTable.id eq commentId }) {
            with(org.jetbrains.exposed.sql.SqlExpressionBuilder) {
                it[likes] = likes + 1
            }
        }
    }

    fun decreaseLikes(commentId: Long) {
        CommentTable.update({ CommentTable.id eq commentId }) {
            with(org.jetbrains.exposed.sql.SqlExpressionBuilder) {
                it[likes] = likes - 1
            }
        }
    }
}
