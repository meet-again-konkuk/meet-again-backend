package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.member.domain.Members

class Comments(val data: List<Comment>) {

    fun extractAuthorIds(): Set<Long> {
        return data.map { it.authorId }.toSet()
    }

    fun extractIds(): List<Long> {
        return data.map { it.id }
    }

    fun groupByRootComment(
        members: Members,
        likeCounts: LikeCounts,
        viewer: Viewer,
    ): List<CommentWithAuthor> {
        val rootComments = RootComments(data.filter { !it.hasParent() })
        val replies = Replies(data.filter { it.hasParent() })
        val commentsWithPreviewReplies = CommentsWithPreviewReplies(rootComments.groupWith(replies))
        return commentsWithPreviewReplies.combineWithAuthors(members, likeCounts, viewer)
    }
}
