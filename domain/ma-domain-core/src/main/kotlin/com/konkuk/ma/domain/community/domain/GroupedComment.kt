package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.member.domain.Members

class GroupedComment(
    val parent: Comment,
    val previewReplies: List<Comment>,
    val remainingReplyCount: Int,
) {
    fun combineWithAuthor(members: Members): CommentWithAuthor {
        return CommentWithAuthor(
            comment = parent,
            nickname = members.findNickname(parent.authorEmail),
            replies = previewReplies.map { reply ->
                ReplyWithAuthor(
                    comment = reply,
                    nickname = members.findNickname(reply.authorEmail),
                )
            },
            remainingReplyCount = remainingReplyCount,
        )
    }
}
