package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.member.domain.Members

class CommentsWithPreviewReplies(val data: List<CommentWithPreviewReplies>) {

    fun combineWithAuthors(
        members: Members,
        likeCounts: LikeCounts,
        viewer: Viewer,
    ): List<CommentWithAuthor> {
        return data.map { it.combineWithAuthor(members, likeCounts, viewer) }
    }
}
