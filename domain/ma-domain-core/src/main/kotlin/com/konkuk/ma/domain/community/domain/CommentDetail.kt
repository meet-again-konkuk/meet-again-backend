package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.member.domain.Members

class CommentDetail(
    val rootComment: Comment,
    val replies: Replies,
) {
    fun combineWithAuthor(members: Members): CommentWithAuthor {
        return CommentWithAuthor(
            comment = rootComment,
            nickname = members.findNickname(rootComment.authorEmail),
            replies = replies.combineWithAuthors(members),
            remainingReplyCount = 0,
        )
    }

    fun extractAuthorEmails(): Set<String> {
        return replies.extractAuthorEmails() + rootComment.authorEmail
    }
}
