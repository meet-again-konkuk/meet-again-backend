package com.konkuk.ma.domain.community.domain

class Replies(data: List<Comment>) {

    private val byParentId: Map<Long, List<Comment>> = data.groupBy { it.parentCommentId!! }

    fun previewFor(parent: Comment): GroupedComment {
        val sorted = findReplies(parent.id)
        return GroupedComment(
            parent = parent,
            previewReplies = sorted.take(PREVIEW_COUNT),
            remainingReplyCount = (sorted.size - PREVIEW_COUNT).coerceAtLeast(0),
        )
    }

    private fun findReplies(parentId: Long): List<Comment> = byParentId[parentId].orEmpty()
        .sortedByDescending { it.createdDate }

    companion object {
        private const val PREVIEW_COUNT = 3
    }
}
