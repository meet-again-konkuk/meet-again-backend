package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.common.domain.page.PageResult
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.Posts

interface PostQueryRepository {
    fun find(category: PostCategory, page: Int): PageResult<Posts>
}
