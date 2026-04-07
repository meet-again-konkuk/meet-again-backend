package com.konkuk.ma.domain.community.repository

import com.konkuk.ma.domain.common.domain.page.PageRequest
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
    override fun find(category: PostCategory, pageRequest: PageRequest): PageResult<Posts> {
        val entities = postQueryDao.find(category.name, pageRequest.size, pageRequest.offset)
        val totalCount = postQueryDao.count(category.name)

        return PageResult(
            data = Posts(entities.map { it.toDomain() }),
            totalCount = totalCount,
            currentPage = pageRequest.page,
            pageSize = pageRequest.size,
        )
    }
}
