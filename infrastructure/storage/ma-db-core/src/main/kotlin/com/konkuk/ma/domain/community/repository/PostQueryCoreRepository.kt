package com.konkuk.ma.domain.community.repository

import com.konkuk.ma.domain.common.domain.page.PageResult
import com.konkuk.ma.domain.community.dao.PostQueryDao
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.Posts
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import org.springframework.stereotype.Repository

@Repository
class PostQueryCoreRepository(
    private val postQueryDao: PostQueryDao,
) : PostQueryRepository {
    override fun find(category: PostCategory, page: Int): PageResult<Posts> {
        val pageSize = Posts.PAGE_SIZE
        val offset = page.toLong() * pageSize
        val entities = postQueryDao.find(category.name, pageSize, offset)
        val totalCount = postQueryDao.count(category.name)

        return PageResult(
            data = Posts(entities.map { it.toDomain() }),
            totalCount = totalCount,
            currentPage = page,
            pageSize = pageSize,
        )
    }
}
