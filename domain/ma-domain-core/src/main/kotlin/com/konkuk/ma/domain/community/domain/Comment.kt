package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.exception.AccessDeniedException
import com.konkuk.ma.exception.InvalidStateException
import com.konkuk.ma.exception.EntityType
import java.time.LocalDateTime

class Comment(
    val id: Long = 0L,
    val postId: Long,
    val authorId: Long,
    val content: String,
    val parentCommentId: Long? = null,
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

    fun validateOwnership(memberId: Long) {
        if (authorId != memberId) {
            throw AccessDeniedException(EntityType.COMMUNITY_COMMENT, authorId.toString(), memberId.toString())
        }
    }

    companion object {
        private const val DELETED_CONTENT = "삭제된 댓글입니다."
    }
}
