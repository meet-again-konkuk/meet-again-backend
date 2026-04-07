package com.konkuk.ma.domain.community.fixture

import com.konkuk.ma.domain.community.domain.Post
import com.konkuk.ma.domain.community.domain.PostCategory
import java.time.LocalDateTime

object PostFixture {
    fun create(
        id: Long = 1L,
        authorEmail: String = "author@example.com",
        category: PostCategory = PostCategory.SUCCESS_STORY,
        title: String = "테스트 게시글",
        content: String = "테스트 내용입니다.",
        likes: Int = 0,
        comments: Int = 0,
        createdDate: LocalDateTime = LocalDateTime.now(),
    ): Post {
        return Post(
            id = id,
            authorEmail = authorEmail,
            category = category,
            title = title,
            content = content,
            likes = likes,
            comments = comments,
            createdDate = createdDate,
        )
    }
}
