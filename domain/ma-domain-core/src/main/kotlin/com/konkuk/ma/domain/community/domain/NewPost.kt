package com.konkuk.ma.domain.community.domain

class NewPost(
    val authorEmail: String,
    val category: PostCategory,
    val title: String,
    val content: String,
) {
    init {
        validateTitle()
        validateContent()
    }

    private fun validateTitle() {
        require(title.isNotBlank()) { "게시글 제목은 비어있을 수 없습니다." }
        require(title.length <= MAX_TITLE_LENGTH) { "게시글 제목은 ${MAX_TITLE_LENGTH}자 이하여야 합니다." }
    }

    private fun validateContent() {
        require(content.isNotBlank()) { "게시글 내용은 비어있을 수 없습니다." }
    }

    companion object {
        private const val MAX_TITLE_LENGTH = 40
    }
}
