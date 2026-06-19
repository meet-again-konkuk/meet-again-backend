package com.konkuk.ma.domain.community.fixture

import com.konkuk.ma.domain.community.domain.CommentLike

object CommentLikeFixture {
    fun create(
        id: Long = 1L,
        commentId: Long = 1L,
        memberId: Long = 1L,
    ): CommentLike {
        return CommentLike(
            id = id,
            commentId = commentId,
            memberId = memberId,
        )
    }
}
