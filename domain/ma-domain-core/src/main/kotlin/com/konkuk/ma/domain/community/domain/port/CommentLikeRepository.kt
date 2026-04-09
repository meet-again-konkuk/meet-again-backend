package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.community.domain.CommentLike

interface CommentLikeRepository {
    fun save(commentLike: CommentLike): Long
    fun delete(commentId: Long, memberEmail: Email)
}
