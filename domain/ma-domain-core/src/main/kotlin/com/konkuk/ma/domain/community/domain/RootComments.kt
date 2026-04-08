package com.konkuk.ma.domain.community.domain

class RootComments(val data: List<Comment>) {

    fun groupWith(replies: Replies): List<GroupedComment> {
        return data.map { parent ->
            replies.previewFor(parent.id)
                .let { (preview, remaining) ->
                    GroupedComment(
                        parent = parent,
                        previewReplies = preview,
                        remainingReplyCount = remaining,
                    )
                }
        }
    }
}
