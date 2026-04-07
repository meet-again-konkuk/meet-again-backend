package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.common.domain.page.PageResult
import com.konkuk.ma.domain.community.domain.Posts

class PostsResponse(
    val posts: List<PostResponse>,
    val hasNext: Boolean,
) {
    companion object {
        fun from(pageResult: PageResult<Posts>): PostsResponse {
            return PostsResponse(
                posts = pageResult.data.data.map { PostResponse.from(it) },
                hasNext = pageResult.hasNext(),
            )
        }
    }
}
