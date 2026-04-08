package com.konkuk.ma.domain.community.domain

class RootComments(val data: List<Comment>) {

    fun groupWith(replies: Replies): List<CommentWithPreviewReplies> {
        return data.map { parent -> replies.previewFor(parent) }
    }
}
