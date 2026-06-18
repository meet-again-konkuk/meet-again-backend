package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.community.domain.PostLike

interface PostLikeRepository {
    fun save(postLike: PostLike): Long
    fun find(memberEmail: Email): List<PostLike>
    fun count(postId: Long): Int
    fun count(postIds: List<Long>): Map<Long, Int>
    fun delete(postId: Long, memberEmail: Email)
    fun deleteByMember(memberEmail: Email)
}
