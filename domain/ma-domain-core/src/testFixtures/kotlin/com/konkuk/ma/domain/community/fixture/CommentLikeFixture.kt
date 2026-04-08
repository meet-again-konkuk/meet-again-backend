package com.konkuk.ma.domain.community.fixture

import com.konkuk.ma.domain.community.domain.CommentLike

object CommentLikeFixture {
    fun create(
        id: Long = 1L,
        commentId: Long = 1L,
        memberEmail: String = "commenter@example.com",
    ): CommentLike {
        return CommentLike(
            id = id,
            commentId = commentId,
            memberEmail = memberEmail,
        )
    }
}
