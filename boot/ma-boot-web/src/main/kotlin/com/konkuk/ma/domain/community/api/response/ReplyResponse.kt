package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.common.domain.TimeAgoCalculator
import com.konkuk.ma.domain.community.domain.ReplyWithAuthor

class ReplyResponse(
    val id: Long,
    val nickname: String,
    val content: String,
    val likes: Int,
    val timeAgo: String,
    val likedByMe: Boolean,
    val isMine: Boolean,
) {
    companion object {
        fun from(replyWithAuthor: ReplyWithAuthor): ReplyResponse {
            val comment = replyWithAuthor.comment
            return ReplyResponse(
                id = comment.id,
                nickname = replyWithAuthor.nickname,
                content = comment.displayContent(),
                likes = replyWithAuthor.likeCount,
                timeAgo = TimeAgoCalculator.calculate(comment.createdDate),
                likedByMe = replyWithAuthor.likedByMe,
                isMine = replyWithAuthor.isMine,
            )
        }
    }
}
