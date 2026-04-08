package com.konkuk.ma.domain.community.domain

class Replies(data: List<Comment>) {

    private val byParentId: Map<Long, List<Comment>> = data.groupBy { it.parentCommentId!! }

    fun previewFor(parent: Comment): GroupedComment {
        val sorted = byParentId[parent.id].orEmpty()
            .sortedByDescending { it.createdDate }
        return GroupedComment(
            parent = parent,
            previewReplies = sorted.take(PREVIEW_COUNT),
            remainingReplyCount = (sorted.size - PREVIEW_COUNT).coerceAtLeast(0),
        )
    }

    companion object {
        private const val PREVIEW_COUNT = 3
    }
}
