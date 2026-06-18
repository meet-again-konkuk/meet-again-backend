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

    fun delete(id: Long) {
        CommentTable.update({ CommentTable.id eq id }) {
            it[deleted] = true
        }
    }

    fun anonymizeAuthor(oldEmail: String, newEmail: String) {
        CommentTable.update({ CommentTable.authorEmail eq oldEmail }) {
            it[authorEmail] = newEmail
            it[lastModifiedBy] = newEmail
        }
    }
}
