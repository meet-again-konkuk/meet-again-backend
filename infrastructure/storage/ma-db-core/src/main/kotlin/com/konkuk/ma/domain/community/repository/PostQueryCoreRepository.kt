package com.konkuk.ma.domain.community.repository

import com.konkuk.ma.domain.common.domain.page.CursorRequest
import com.konkuk.ma.domain.common.domain.page.CursorResult
import com.konkuk.ma.domain.community.dao.PostQueryDao
import com.konkuk.ma.domain.community.domain.Post
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.Posts
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import org.springframework.stereotype.Repository

@Repository
class PostQueryCoreRepository(
    private val postQueryDao: PostQueryDao,
) : PostQueryRepository {
    override fun find(category: PostCategory?, cursorRequest: CursorRequest): CursorResult<Posts> {
        val entities = postQueryDao.find(category?.name, cursorRequest.cursorId, cursorRequest.fetchSize)
        val posts = entities.map { it.toDomain() }

        val cursorResult = CursorResult.of(posts, cursorRequest.size) { it.id }

        return CursorResult(
            data = Posts(cursorResult.data),
            hasNext = cursorResult.hasNext,
            nextCursorId = cursorResult.nextCursorId,
        )
    }
}
