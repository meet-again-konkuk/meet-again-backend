package com.konkuk.ma.domain.community.repository

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.common.domain.page.CursorIdCondition
import com.konkuk.ma.domain.common.domain.page.CursorResult
import com.konkuk.ma.domain.community.dao.PostQueryDao
import com.konkuk.ma.domain.community.domain.Post
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
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

    override fun find(authorEmail: Email): List<Post> {
        return postQueryDao.find(authorEmail.value).map { it.toDomain() }
    }

    override fun findOne(id: Long): Post {
        return postQueryDao.findOne(id)?.toDomain()
            ?: throw EntityNotFoundException(EntityType.COMMUNITY_POST, id.toString())
    }

    override fun exists(id: Long): Boolean {
        return postQueryDao.exists(id)
    }
}
