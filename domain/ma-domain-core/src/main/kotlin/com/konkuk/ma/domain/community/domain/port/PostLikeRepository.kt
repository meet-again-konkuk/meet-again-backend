package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.community.domain.PostLike

interface PostLikeRepository {
    fun save(postLike: PostLike): Long
    fun delete(postId: Long, memberEmail: Email)
}
