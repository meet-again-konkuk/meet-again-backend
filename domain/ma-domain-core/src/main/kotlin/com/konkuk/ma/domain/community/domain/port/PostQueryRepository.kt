package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.common.domain.page.CursorRequest
import com.konkuk.ma.domain.common.domain.page.CursorResult
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.Posts

interface PostQueryRepository {
    fun find(category: PostCategory, cursorRequest: CursorRequest): CursorResult<Posts>
}
