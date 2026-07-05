package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.community.domain.CommentLike

interface CommentLikeRepository {
    /** 멱등 저장 — 같은 회원이 같은 댓글에 중복 저장하면 무시된다 (복합 유니크 기반) */
    fun save(commentLike: CommentLike)
    fun find(memberId: Long): List<CommentLike>
    fun count(commentId: Long): Int
    fun count(commentIds: List<Long>): Map<Long, Int>
    fun findLikedCommentIds(memberId: Long, commentIds: List<Long>): Set<Long>
    fun delete(commentId: Long, memberId: Long)
    fun deleteByMember(memberId: Long)
}
