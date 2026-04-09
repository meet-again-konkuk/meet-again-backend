package com.konkuk.ma.domain.community.fixture

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.community.domain.Comment
import java.time.LocalDateTime

object CommentFixture {
    fun create(
        id: Long = 1L,
        postId: Long = 1L,
        authorEmail: String = "commenter@example.com",
        content: String = "테스트 댓글 내용입니다.",
        parentCommentId: Long? = null,
        likes: Int = 0,
        createdDate: LocalDateTime = LocalDateTime.now(),
        deleted: Boolean = false,
    ): Comment {
        return Comment(
            id = id,
            postId = postId,
            authorEmail = Email(authorEmail),
            content = content,
            parentCommentId = parentCommentId,
            likes = likes,
            createdDate = createdDate,
            deleted = deleted,
        )
    }
}
