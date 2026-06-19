package com.konkuk.ma.domain.community.fixture

import com.konkuk.ma.domain.community.domain.PostLike

object PostLikeFixture {
    fun create(
        id: Long = 1L,
        postId: Long = 1L,
        memberId: Long = 1L,
    ): PostLike {
        return PostLike(
            id = id,
            postId = postId,
            memberId = memberId,
        )
    }
}
