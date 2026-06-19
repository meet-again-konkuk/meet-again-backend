package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.community.domain.PostLike

interface PostLikeRepository {
    fun save(postLike: PostLike): Long
    fun find(memberId: Long): List<PostLike>
    fun count(postId: Long): Int
    fun count(postIds: List<Long>): Map<Long, Int>
    fun delete(postId: Long, memberId: Long)
    fun deleteByMember(memberId: Long)
}
