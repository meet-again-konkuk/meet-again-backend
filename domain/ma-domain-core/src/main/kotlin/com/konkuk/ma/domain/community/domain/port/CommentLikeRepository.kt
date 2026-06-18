package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.community.domain.CommentLike

interface CommentLikeRepository {
    fun save(commentLike: CommentLike): Long
    fun find(memberEmail: Email): List<CommentLike>
    fun count(commentId: Long): Int
    fun count(commentIds: List<Long>): Map<Long, Int>
    fun delete(commentId: Long, memberEmail: Email)
    fun deleteByMember(memberEmail: Email)
}
