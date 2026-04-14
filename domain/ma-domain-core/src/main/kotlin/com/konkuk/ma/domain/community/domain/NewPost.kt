package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.exception.InvalidValueException

class NewPost(
    authorEmail: String,
    val category: PostCategory,
    val title: String,
    val content: String,
) {
    val authorEmail: Email = Email(authorEmail)

    init {
        validateTitle()
        validateContent()
    }

    private fun validateTitle() {
        if (title.isBlank()) throw InvalidValueException(NewPost::class, title, "게시글 제목은 비어있을 수 없습니다.")
        if (title.length > MAX_TITLE_LENGTH) throw InvalidValueException(NewPost::class, title, "게시글 제목은 ${MAX_TITLE_LENGTH}자 이하여야 합니다.")
    }

    private fun validateContent() {
        if (content.isBlank()) throw InvalidValueException(NewPost::class, content, "게시글 내용은 비어있을 수 없습니다.")
        if (content.length > MAX_CONTENT_LENGTH) throw InvalidValueException(NewPost::class, content, "게시글 내용은 ${MAX_CONTENT_LENGTH}자 이하여야 합니다.")
    }

    companion object {
        const val MAX_TITLE_LENGTH = 30
        const val MAX_CONTENT_LENGTH = 2000
    }
}
