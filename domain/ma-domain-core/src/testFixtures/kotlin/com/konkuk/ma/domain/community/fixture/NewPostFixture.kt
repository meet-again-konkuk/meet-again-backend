package com.konkuk.ma.domain.community.fixture

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.community.domain.NewPost
import com.konkuk.ma.domain.community.domain.PostCategory

object NewPostFixture {
    fun create(
        authorEmail: String = "author@example.com",
        category: PostCategory = PostCategory.SUCCESS_STORY,
        title: String = "테스트 게시글",
        content: String = "테스트 내용입니다.",
    ): NewPost {
        return NewPost(
            authorEmail = Email(authorEmail),
            category = category,
            title = title,
            content = content,
        )
    }
}
