package com.konkuk.ma.domain.community.domain

class NewComment(
    val postId: Long,
    val authorEmail: String,
    val content: String,
    val parentCommentId: Long? = null,
) {
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
