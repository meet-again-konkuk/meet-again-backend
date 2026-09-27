package com.konkuk.ma.domain.community.fixture

import com.konkuk.ma.domain.community.domain.NewPost
import com.konkuk.ma.domain.community.domain.PostCategory

object NewPostFixture {
    fun create(
        authorId: Long = 1L,
        category: PostCategory = PostCategory.SUCCESS_STORY,
        title: String = "테스트 게시글",
        content: String = "테스트 내용입니다.",
    ): NewPost {
        return NewPost(
            authorId = authorId,
            details = PostDetailsFixture.create(category = category, title = title, content = content),
        )
    }
}
