package com.konkuk.ma.domain.community.fixture

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.community.domain.PostLike

object PostLikeFixture {
    fun create(
        id: Long = 1L,
        postId: Long = 1L,
        memberEmail: String = "liker@example.com",
    ): PostLike {
        return PostLike(
            id = id,
            postId = postId,
            memberEmail = Email(memberEmail),
        )
    }
}
