package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.community.exception.ReplyDepthExceededException
import java.time.LocalDateTime

class Comment(
    val id: Long = 0L,
    val postId: Long,
    val authorEmail: String,
    val content: String,
    val parentCommentId: Long? = null,
    val createdDate: LocalDateTime = LocalDateTime.now(),
) {
    fun hasParent(): Boolean = parentCommentId != null

    fun validateCanBeParent() {
        if (hasParent()) {
            throw ReplyDepthExceededException(id)
        }
    }
}
