package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.common.domain.Email

class NewComment(
    val postId: Long,
    authorEmail: String,
    val content: String,
    val parentCommentId: Long? = null,
) {
    val authorEmail: Email = Email(authorEmail)

    init {
        validateContent()
    }

    private fun validateContent() {
        require(content.isNotBlank()) { "댓글 내용은 비어있을 수 없습니다." }
        require(content.length <= MAX_CONTENT_LENGTH) { "댓글 내용은 ${MAX_CONTENT_LENGTH}자 이하여야 합니다." }
    }

    fun hasParent(): Boolean = parentCommentId != null

    companion object {
        const val MAX_CONTENT_LENGTH = 500
    }
}
