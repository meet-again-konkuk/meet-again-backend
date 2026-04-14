package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.InvalidStateException
import com.konkuk.ma.exception.EntityType
import java.time.LocalDateTime

class Comment(
    val id: Long = 0L,
    val postId: Long,
    val authorEmail: Email,
    val content: String,
    val parentCommentId: Long? = null,
    val likes: Int = 0,
    val createdDate: LocalDateTime = LocalDateTime.now(),
    val deleted: Boolean = false,
) {
    fun displayContent(): String {
        if (deleted) return DELETED_CONTENT
        return content
    }

    fun hasParent(): Boolean = parentCommentId != null

    fun validateIsRootComment() {
        if (hasParent()) {
            throw InvalidStateException(Comment::class, id, "루트 댓글이 아닙니다.")
        }
    }

    fun validateOwnership(email: Email) {
        if (authorEmail != email) {
            throw AccessDeniedException(EntityType.COMMUNITY_COMMENT, authorEmail.value, email.value)
        }
    }

    companion object {
        private const val DELETED_CONTENT = "삭제된 댓글입니다."
    }
}
