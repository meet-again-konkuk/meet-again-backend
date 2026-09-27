package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.community.domain.Comment

interface CommentQueryRepository {
    fun findOne(id: Long): Comment
    fun find(postId: Long): List<Comment>
    fun findByAuthor(authorId: Long): List<Comment>
    fun findReplies(parentCommentId: Long): List<Comment>
    fun count(postIds: List<Long>): Map<Long, Int>
}
