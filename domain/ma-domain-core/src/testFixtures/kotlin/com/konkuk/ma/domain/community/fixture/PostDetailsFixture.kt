package com.konkuk.ma.domain.community.fixture

import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.PostDetails

object PostDetailsFixture {
    fun create(
        category: PostCategory = PostCategory.SUCCESS_STORY,
        title: String = "수정된 게시글",
        content: String = "수정된 내용입니다.",
    ): PostDetails {
        return PostDetails(
            category = category,
            title = title,
            content = content,
        )
    }
}
