package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.common.domain.page.CursorRequest
import com.konkuk.ma.domain.common.domain.page.CursorResult
import com.konkuk.ma.domain.community.domain.Post
import com.konkuk.ma.domain.community.domain.PostCategory

interface PostQueryRepository {
    fun find(category: PostCategory?, cursorRequest: CursorRequest): CursorResult<List<Post>>
}
