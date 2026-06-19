package com.konkuk.ma.domain.community.fixture

import com.konkuk.ma.domain.community.domain.Comment
import java.time.LocalDateTime

object CommentFixture {
    fun create(
        id: Long = 1L,
        postId: Long = 1L,
        authorId: Long = 1L,
        content: String = "테스트 댓글 내용입니다.",
        parentCommentId: Long? = null,
        createdDate: LocalDateTime = LocalDateTime.now(),
        deleted: Boolean = false,
    ): Comment {
        return Comment(
            id = id,
            postId = postId,
            authorId = authorId,
            content = content,
            parentCommentId = parentCommentId,
            createdDate = createdDate,
            deleted = deleted,
        )
    }
}
