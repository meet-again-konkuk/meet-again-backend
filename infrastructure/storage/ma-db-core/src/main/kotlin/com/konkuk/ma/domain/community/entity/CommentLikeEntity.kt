package com.konkuk.ma.domain.community.entity

import com.konkuk.ma.domain.community.domain.CommentLike
import com.konkuk.ma.domain.community.entity.table.CommentLikeTable
import org.jetbrains.exposed.sql.ResultRow

class CommentLikeEntity(
    val id: Long,
    val commentId: Long,
    val memberEmail: String,
) {
    fun toDomain(): CommentLike {
        return CommentLike(
            id = id,
            commentId = commentId,
            memberEmail = memberEmail,
        )
    }

    companion object {
        fun from(row: ResultRow): CommentLikeEntity {
            return CommentLikeEntity(
                id = row[CommentLikeTable.id].value,
                commentId = row[CommentLikeTable.commentId],
                memberEmail = row[CommentLikeTable.memberEmail],
            )
        }
    }
}
