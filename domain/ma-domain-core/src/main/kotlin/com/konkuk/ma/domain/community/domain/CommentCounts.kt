package com.konkuk.ma.domain.community.domain

class CommentCounts(val data: Map<Long, Int>) {

    fun countOf(postId: Long): Int = data[postId] ?: 0

    companion object {
        fun from(countById: Map<Long, Int>): CommentCounts = CommentCounts(countById)
    }
}
