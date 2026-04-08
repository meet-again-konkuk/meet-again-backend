package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.member.domain.Members

class CommentWithPreviewReplies(
    val parent: Comment,
    val previewReplies: Replies,
    val remainingReplyCount: Int,
) {
    fun combineWithAuthor(members: Members): CommentWithAuthor {
        return CommentWithAuthor(
            comment = parent,
            nickname = members.findNickname(parent.authorEmail),
            replies = previewReplies.combineWithAuthors(members),
            remainingReplyCount = remainingReplyCount,
        )
    }
}
