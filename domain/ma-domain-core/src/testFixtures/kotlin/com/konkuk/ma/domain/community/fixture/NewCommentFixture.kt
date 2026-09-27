package com.konkuk.ma.domain.community.fixture

import com.konkuk.ma.domain.community.domain.NewComment

object NewCommentFixture {
    fun create(
        postId: Long = 1L,
        authorId: Long = 1L,
        content: String = "테스트 댓글 내용입니다.",
        parentCommentId: Long? = null,
    ): NewComment {
        return NewComment(
            postId = postId,
            authorId = authorId,
            content = content,
            parentCommentId = parentCommentId,
        )
    }
}
