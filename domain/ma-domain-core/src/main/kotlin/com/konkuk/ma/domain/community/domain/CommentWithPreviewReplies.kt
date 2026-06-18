package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.member.domain.Members

class CommentWithPreviewReplies(
    val parent: Comment,
    val previewReplies: Replies,
    val remainingReplyCount: Int,
) {
    fun combineWithAuthor(members: Members, likeCounts: LikeCounts): CommentWithAuthor {
        return CommentWithAuthor(
            comment = parent,
            nickname = members.findNickname(parent.authorEmail),
            likeCount = likeCounts.countOf(parent.id),
            replies = previewReplies.combineWithAuthors(members, likeCounts),
            remainingReplyCount = remainingReplyCount,
        )
    }
}
