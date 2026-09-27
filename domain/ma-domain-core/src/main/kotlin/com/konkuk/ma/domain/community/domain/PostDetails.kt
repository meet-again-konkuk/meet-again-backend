package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.exception.InvalidValueException

class PostDetails(
    val category: PostCategory,
    val title: String,
    val content: String,
) {
    init {
        validateTitle()
        validateContent()
    }

    private fun validateTitle() {
        if (title.isBlank()) throw InvalidValueException(PostDetails::class, title, "게시글 제목은 비어있을 수 없습니다.")
        if (title.length > MAX_TITLE_LENGTH) throw InvalidValueException(PostDetails::class, title, "게시글 제목은 ${MAX_TITLE_LENGTH}자 이하여야 합니다.")
    }

    private fun validateContent() {
        if (content.isBlank()) throw InvalidValueException(PostDetails::class, content, "게시글 내용은 비어있을 수 없습니다.")
        if (content.length > MAX_CONTENT_LENGTH) throw InvalidValueException(PostDetails::class, content, "게시글 내용은 ${MAX_CONTENT_LENGTH}자 이하여야 합니다.")
    }

    companion object {
        const val MAX_TITLE_LENGTH = 30
        const val MAX_CONTENT_LENGTH = 2000
    }
}
