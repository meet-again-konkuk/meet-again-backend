package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.community.domain.Comment

interface CommentQueryRepository {
    fun findOne(id: Long): Comment
    fun find(postId: Long): List<Comment>
}
