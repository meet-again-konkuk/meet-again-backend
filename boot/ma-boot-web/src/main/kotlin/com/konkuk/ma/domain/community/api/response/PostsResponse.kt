package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.common.domain.page.CursorResult
import com.konkuk.ma.domain.community.domain.Posts
import com.konkuk.ma.support.id.EncryptId

class PostsResponse(
    val posts: List<PostResponse>,
    val hasNext: Boolean,
    @EncryptId(ObfuscationType.COMMUNITY_POST)
    val nextCursorId: Long?,
) {
    companion object {
        fun from(cursorResult: CursorResult<Posts>): PostsResponse {
            return PostsResponse(
                posts = cursorResult.data.data.map { PostResponse.from(it) },
                hasNext = cursorResult.hasNext,
                nextCursorId = cursorResult.nextCursorId,
            )
        }
    }
}
