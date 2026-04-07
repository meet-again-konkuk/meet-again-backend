package com.konkuk.ma.domain.community.fixture

import com.konkuk.ma.domain.community.domain.NewPost
import com.konkuk.ma.domain.community.domain.PostCategory

object NewPostFixture {
    fun create(
        authorEmail: String = "author@example.com",
        authorNickname: String = "작성자",
        category: PostCategory = PostCategory.SUCCESS_STORY,
        title: String = "테스트 게시글",
        content: String = "테스트 내용입니다.",
    ): NewPost {
        return NewPost(
            authorEmail = authorEmail,
            authorNickname = authorNickname,
            category = category,
            title = title,
            content = content,
        )
    }
}
