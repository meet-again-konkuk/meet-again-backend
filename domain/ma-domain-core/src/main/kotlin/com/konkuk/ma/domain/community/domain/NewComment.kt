package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.exception.InvalidValueException

class NewComment(
    val postId: Long,
    val authorId: Long,
    val content: String,
    val parentCommentId: Long? = null,
) {
    init {
        validateContent()
    }

    private fun validateContent() {
        if (content.isBlank()) throw InvalidValueException(NewComment::class, content, "댓글 내용은 비어있을 수 없습니다.")
        if (content.length > MAX_CONTENT_LENGTH) throw InvalidValueException(NewComment::class, content, "댓글 내용은 ${MAX_CONTENT_LENGTH}자 이하여야 합니다.")
    }

    fun hasParent(): Boolean = parentCommentId != null

    companion object {
        const val MAX_CONTENT_LENGTH = 500
    }
}
