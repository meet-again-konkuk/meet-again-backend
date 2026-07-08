package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.common.domain.TimeAgoCalculator
import com.konkuk.ma.domain.community.domain.PostDetail

class PostDetailResponse(
    val id: Long,
    val nickname: String,
    val category: String,
    val title: String,
    val content: String,
    val likes: Int,
    val timeAgo: String,
    val likedByMe: Boolean,
    val isMine: Boolean,
    val comments: List<CommentResponse>,
    val imageUrl: String? = null,
    val thumbnailUrl: String? = null,
) {
    companion object {
        fun from(postDetail: PostDetail): PostDetailResponse {
            val post = postDetail.post
            return PostDetailResponse(
                id = post.id,
                nickname = postDetail.nickname,
                category = post.category.name,
                title = post.title,
                content = post.content,
                likes = postDetail.likeCount,
                timeAgo = TimeAgoCalculator.calculate(post.createdDate),
                likedByMe = postDetail.likedByMe,
                isMine = postDetail.isMine,
                comments = postDetail.comments.map { CommentResponse.from(it) },
                imageUrl = postDetail.imageUrl,
                thumbnailUrl = postDetail.thumbnailUrl,
            )
        }
    }
}
