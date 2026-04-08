package com.konkuk.ma.domain.community.domain

class RootComments(val data: List<Comment>) {

    fun groupWith(allReplies: List<Comment>): List<GroupedComment> {
        val repliesByParentId = allReplies.groupBy { it.parentCommentId!! }

        return data.map { parent ->
            val sorted = repliesByParentId[parent.id].orEmpty()
                .sortedByDescending { it.createdDate }
            GroupedComment(
                parent = parent,
                previewReplies = Replies(sorted.take(PREVIEW_COUNT)),
                remainingReplyCount = (sorted.size - PREVIEW_COUNT).coerceAtLeast(0),
            )
        }
    }

    companion object {
        private const val PREVIEW_COUNT = 3
    }
}
