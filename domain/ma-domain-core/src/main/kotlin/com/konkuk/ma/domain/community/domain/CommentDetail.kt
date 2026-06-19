package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.member.domain.Members

class CommentDetail(
    val rootComment: Comment,
    val replies: Replies,
) {
    fun combineWithAuthor(members: Members, likeCounts: LikeCounts): CommentWithAuthor {
        return CommentWithAuthor(
            comment = rootComment,
            nickname = members.findNickname(rootComment.authorId),
            likeCount = likeCounts.countOf(rootComment.id),
            replies = replies.combineWithAuthors(members, likeCounts),
            remainingReplyCount = 0,
        )
    }

    fun extractAuthorIds(): Set<Long> {
        return replies.extractAuthorIds() + rootComment.authorId
    }

    fun extractIds(): List<Long> {
        return replies.extractIds() + rootComment.id
    }
}
