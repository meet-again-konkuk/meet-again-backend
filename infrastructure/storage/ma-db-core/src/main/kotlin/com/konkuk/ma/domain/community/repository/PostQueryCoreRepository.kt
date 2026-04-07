package com.konkuk.ma.domain.community.repository

import com.konkuk.ma.domain.common.domain.page.CursorIdCondition
import com.konkuk.ma.domain.common.domain.page.CursorResult
import com.konkuk.ma.domain.community.dao.PostQueryDao
import com.konkuk.ma.domain.community.domain.Post
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import org.springframework.stereotype.Repository

@Repository
class PostQueryCoreRepository(
    private val postQueryDao: PostQueryDao,
) : PostQueryRepository {
    override fun find(category: PostCategory?, cursorCondition: CursorIdCondition): CursorResult<List<Post>> {
        val posts = postQueryDao.find(category?.name, cursorCondition.cursorId, cursorCondition.size)
            .map { it.toDomain() }

        return CursorResult.of(posts, cursorCondition.size) { it.id }
    }
}
