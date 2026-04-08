package com.konkuk.ma.domain.community.domain

class Replies(data: List<Comment>) {

    private val byParentId: Map<Long, List<Comment>> = data.groupBy { it.parentCommentId!! }

    fun previewFor(parentId: Long): Pair<List<Comment>, Int> {
        val sorted = byParentId[parentId].orEmpty()
            .sortedByDescending { it.createdDate }
        val preview = sorted.take(PREVIEW_COUNT)
        val remaining = (sorted.size - PREVIEW_COUNT).coerceAtLeast(0)
        return Pair(preview, remaining)
    }

    companion object {
        private const val PREVIEW_COUNT = 3
    }
}
