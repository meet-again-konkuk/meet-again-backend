package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.member.domain.Members

class CommentWithPreviewReplies(
    val parent: Comment,
    val previewReplies: Replies,
    val remainingReplyCount: Int,
) {
    fun combineWithAuthor(
        members: Members,
        likeCounts: LikeCounts,
        viewer: Viewer,
    ): CommentWithAuthor {
        return CommentWithAuthor(
            comment = parent,
            nickname = members.findNickname(parent.authorId),
            likeCount = likeCounts.countOf(parent.id),
            replies = previewReplies.combineWithAuthors(members, likeCounts, viewer),
            remainingReplyCount = remainingReplyCount,
            likedByMe = viewer.isLikedByMe(parent.id),
            isMine = viewer.isMine(parent.authorId),
        )
    }
}
