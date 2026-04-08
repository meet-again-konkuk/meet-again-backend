package com.konkuk.ma.domain.community.entity

import com.konkuk.ma.domain.community.domain.Comment
import com.konkuk.ma.domain.community.entity.table.CommentTable
import org.jetbrains.exposed.sql.ResultRow
import java.time.LocalDateTime

class CommentEntity(
    val id: Long,
    val postId: Long,
    val authorEmail: String,
    val content: String,
    val parentCommentId: Long?,
    val createdDate: LocalDateTime,
) {
    fun toDomain(): Comment {
        return Comment(
            id = id,
            postId = postId,
            authorEmail = authorEmail,
            content = content,
            parentCommentId = parentCommentId,
            createdDate = createdDate,
        )
    }

    companion object {
        fun from(row: ResultRow): CommentEntity {
            return CommentEntity(
                id = row[CommentTable.id].value,
                postId = row[CommentTable.postId],
                authorEmail = row[CommentTable.authorEmail],
                content = row[CommentTable.content],
                parentCommentId = row[CommentTable.parentCommentId],
                createdDate = row[CommentTable.createdDate],
            )
        }
    }
}
