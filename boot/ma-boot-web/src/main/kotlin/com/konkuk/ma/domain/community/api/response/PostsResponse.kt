package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.community.domain.Posts

class PostsResponse(
    val posts: List<PostResponse>,
    val totalCount: Long,
    val currentPage: Int,
    val hasNext: Boolean,
) {
    companion object {
        fun from(posts: Posts): PostsResponse {
            return PostsResponse(
                posts = posts.data.map { PostResponse.from(it) },
                totalCount = posts.totalCount,
                currentPage = posts.currentPage,
                hasNext = posts.hasNext(),
            )
        }
    }
}
