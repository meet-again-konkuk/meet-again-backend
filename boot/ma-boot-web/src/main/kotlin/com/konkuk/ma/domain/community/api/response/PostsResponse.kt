package com.konkuk.ma.domain.community.api.response

import com.konkuk.ma.domain.common.domain.page.CursorResult
import com.konkuk.ma.domain.community.domain.Posts

class PostsResponse(
    val posts: List<PostResponse>,
    val hasNext: Boolean,
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
