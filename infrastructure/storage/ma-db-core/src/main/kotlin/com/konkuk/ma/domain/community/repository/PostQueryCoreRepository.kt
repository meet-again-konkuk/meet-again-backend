package com.konkuk.ma.domain.community.repository

import com.konkuk.ma.domain.community.dao.PostQueryDao
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.Posts
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import org.springframework.stereotype.Repository

@Repository
class PostQueryCoreRepository(
    private val postQueryDao: PostQueryDao,
) : PostQueryRepository {
    override fun find(category: PostCategory, page: Int): Posts {
        val offset = page.toLong() * Posts.PAGE_SIZE
        val entities = postQueryDao.find(category.name, Posts.PAGE_SIZE, offset)
        val totalCount = postQueryDao.count(category.name)

        return Posts(
            data = entities.map { it.toDomain() },
            totalCount = totalCount,
            currentPage = page,
        )
    }
}
