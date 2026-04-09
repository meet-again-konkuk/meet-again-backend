package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.member.domain.Members

class Comments(val data: List<Comment>) {

    fun extractAuthorEmails(): Set<Email> {
        return data.map { it.authorEmail }.toSet()
    }

    fun groupByRootComment(members: Members): List<CommentWithAuthor> {
        val rootComments = RootComments(data.filter { !it.hasParent() })
        val replies = Replies(data.filter { it.hasParent() })
        val commentsWithPreviewReplies = CommentsWithPreviewReplies(rootComments.groupWith(replies))
        return commentsWithPreviewReplies.combineWithAuthors(members)
    }
}
