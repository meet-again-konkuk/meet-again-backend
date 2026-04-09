package com.konkuk.ma.domain.community.fixture

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.community.domain.NewComment

object NewCommentFixture {
    fun create(
        postId: Long = 1L,
        authorEmail: String = "commenter@example.com",
        content: String = "테스트 댓글 내용입니다.",
        parentCommentId: Long? = null,
    ): NewComment {
        return NewComment(
            postId = postId,
            authorEmail = Email(authorEmail),
            content = content,
            parentCommentId = parentCommentId,
        )
    }
}
