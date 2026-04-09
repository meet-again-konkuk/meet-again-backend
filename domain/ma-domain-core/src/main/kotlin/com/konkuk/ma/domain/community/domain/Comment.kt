package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.community.exception.CommentAccessDeniedException
import com.konkuk.ma.domain.community.exception.NotRootCommentException
import com.konkuk.ma.domain.community.exception.ReplyDepthExceededException
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

    fun validateCanBeParent() {
        if (hasParent()) {
            throw ReplyDepthExceededException(id)
        }
    }

    fun validateIsRootComment() {
        if (hasParent()) {
            throw NotRootCommentException(id)
        }
    }

    fun validateOwnership(email: Email) {
        if (authorEmail != email) {
            throw CommentAccessDeniedException(id, authorEmail.value, email.value)
        }
    }

    companion object {
        private const val DELETED_CONTENT = "삭제된 댓글입니다."
    }
}
