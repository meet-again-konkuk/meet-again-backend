package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.community.domain.CommentLike

interface CommentLikeRepository {
    fun exists(commentId: Long, memberId: Long): Boolean
    fun save(commentLike: CommentLike)
    fun find(memberId: Long): List<CommentLike>
    fun count(commentId: Long): Int
    fun count(commentIds: List<Long>): Map<Long, Int>
    fun findLikedCommentIds(memberId: Long, commentIds: List<Long>): Set<Long>
    fun delete(commentId: Long, memberId: Long)
    fun deleteByMember(memberId: Long)
}
