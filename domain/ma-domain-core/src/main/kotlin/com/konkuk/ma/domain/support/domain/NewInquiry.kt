package com.konkuk.ma.domain.support.domain

import com.konkuk.ma.domain.common.domain.Email

class NewInquiry(
    authorEmail: String,
    val title: String,
    val content: String,
) {
    val authorEmail: Email = Email(authorEmail)

    init {
        validateTitle()
        validateContent()
    }

    private fun validateTitle() {
        require(title.isNotBlank()) { "문의 제목은 비어있을 수 없습니다." }
        require(title.length <= MAX_TITLE_LENGTH) { "문의 제목은 ${MAX_TITLE_LENGTH}자 이하여야 합니다." }
    }

    private fun validateContent() {
        require(content.isNotBlank()) { "문의 내용은 비어있을 수 없습니다." }
        require(content.length <= MAX_CONTENT_LENGTH) { "문의 내용은 ${MAX_CONTENT_LENGTH}자 이하여야 합니다." }
    }

    companion object {
        const val MAX_TITLE_LENGTH = 50
        const val MAX_CONTENT_LENGTH = 500
    }
}
