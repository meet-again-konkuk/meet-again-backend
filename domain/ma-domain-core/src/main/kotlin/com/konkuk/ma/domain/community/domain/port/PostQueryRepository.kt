package com.konkuk.ma.domain.community.domain.port

import com.konkuk.ma.domain.common.domain.page.CursorIdCondition
import com.konkuk.ma.domain.common.domain.page.CursorResult
import com.konkuk.ma.domain.community.domain.Post
import com.konkuk.ma.domain.community.domain.PostCategory

interface PostQueryRepository {
    fun find(category: PostCategory?, cursorCondition: CursorIdCondition): CursorResult<List<Post>>
    fun findByAuthor(authorId: Long): List<Post>
    fun findOne(id: Long): Post
    fun exists(id: Long): Boolean
}
