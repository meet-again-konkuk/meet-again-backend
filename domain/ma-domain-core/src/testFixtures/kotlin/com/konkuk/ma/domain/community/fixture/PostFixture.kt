package com.konkuk.ma.domain.community.fixture

import com.konkuk.ma.domain.community.domain.Post
import com.konkuk.ma.domain.community.domain.PostCategory
import java.time.LocalDateTime

object PostFixture {
    fun create(
        id: Long = 1L,
        authorId: Long = 1L,
        category: PostCategory = PostCategory.SUCCESS_STORY,
        title: String = "테스트 게시글",
        content: String = "테스트 내용입니다.",
        createdDate: LocalDateTime = LocalDateTime.now(),
    ): Post {
        return Post(
            id = id,
            authorId = authorId,
            category = category,
            title = title,
            content = content,
            createdDate = createdDate,
        )
    }
}
