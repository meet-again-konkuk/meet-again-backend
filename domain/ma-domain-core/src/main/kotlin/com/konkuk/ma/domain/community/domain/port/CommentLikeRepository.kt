package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.community.domain.CommentLike

interface CommentLikeRepository {
    fun findOneOrNull(commentId: Long, memberEmail: String): CommentLike?
    fun save(commentLike: CommentLike): Long
    fun delete(commentId: Long, memberEmail: String)
}
