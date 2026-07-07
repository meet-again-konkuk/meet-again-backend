package com.konkuk.ma.domain.community.api.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.konkuk.ma.domain.common.domain.TimeAgoCalculator
import com.konkuk.ma.domain.community.domain.PostWithAuthor

class PostResponse(
    val id: Long,
    val nickname: String,
    val category: String,
    val title: String,
    val content: String,
    val likes: Int,
    val timeAgo: String,
    val likedByMe: Boolean,
    val isMine: Boolean,
    val commentCount: Int,
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val imageUrl: String? = null,
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val thumbnailUrl: String? = null,
) {
    companion object {
        fun from(postWithAuthor: PostWithAuthor): PostResponse {
            val post = postWithAuthor.post
            return PostResponse(
                id = post.id,
                nickname = postWithAuthor.nickname,
                category = post.category.name,
                title = post.title,
                content = post.content,
                likes = postWithAuthor.likeCount,
                timeAgo = TimeAgoCalculator.calculate(post.createdDate),
                likedByMe = postWithAuthor.likedByMe,
                isMine = postWithAuthor.isMine,
                commentCount = postWithAuthor.commentCount,
                imageUrl = postWithAuthor.imageUrl,
                thumbnailUrl = postWithAuthor.thumbnailUrl,
            )
        }
    }
}
