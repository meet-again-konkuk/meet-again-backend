package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.common.domain.TimeAgoCalculator
import com.konkuk.ma.domain.community.domain.CommentWithAuthor

class CommentResponse(
    val id: Long,
    val nickname: String,
    val content: String,
    val likes: Int,
    val timeAgo: String,
    val replies: List<ReplyResponse>,
    val remainingReplyCount: Int,
    val likedByMe: Boolean,
    val isMine: Boolean,
) {
    companion object {
        fun from(commentWithAuthor: CommentWithAuthor): CommentResponse {
            val comment = commentWithAuthor.comment
            return CommentResponse(
                id = comment.id,
                nickname = commentWithAuthor.nickname,
                content = comment.displayContent(),
                likes = commentWithAuthor.likeCount,
                timeAgo = TimeAgoCalculator.calculate(comment.createdDate),
                replies = commentWithAuthor.replies.map { ReplyResponse.from(it) },
                remainingReplyCount = commentWithAuthor.remainingReplyCount,
                likedByMe = commentWithAuthor.likedByMe,
                isMine = commentWithAuthor.isMine,
            )
        }
    }
}
