package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.community.domain.Post

class PostResponse(
    val id: Long,
    val nickname: String,
    val category: String,
    val title: String,
    val content: String,
    val likes: Int,
    val comments: Int,
    val timeAgo: String,
) {
    companion object {
        fun from(post: Post): PostResponse {
            return PostResponse(
                id = post.id,
                nickname = post.authorNickname,
                category = post.category.name,
                title = post.title,
                content = post.content,
                likes = post.likes,
                comments = post.comments,
                timeAgo = post.calculateTimeAgo(),
            )
        }
    }
}
