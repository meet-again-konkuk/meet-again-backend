package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.community.domain.PostLike

interface PostLikeRepository {
    /** 멱등 저장 — 같은 회원이 같은 게시글에 중복 저장하면 무시된다 (복합 유니크 기반) */
    fun save(postLike: PostLike)
    fun find(memberId: Long): List<PostLike>
    fun count(postId: Long): Int
    fun count(postIds: List<Long>): Map<Long, Int>
    fun delete(postId: Long, memberId: Long)
    fun deleteByMember(memberId: Long)
}
