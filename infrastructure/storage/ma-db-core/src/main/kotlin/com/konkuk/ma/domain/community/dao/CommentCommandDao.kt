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
            it[authorId] = newComment.authorId
            it[content] = newComment.content
            it[parentCommentId] = newComment.parentCommentId
            it[createdBy] = newComment.authorId.toString()
            it[lastModifiedBy] = newComment.authorId.toString()
        }.value
    }

    fun delete(id: Long) {
        CommentTable.update({ CommentTable.id eq id }) {
            it[deleted] = true
        }
    }
}
