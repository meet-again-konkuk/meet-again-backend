package com.konkuk.ma.domain.community.repository

import com.konkuk.ma.domain.community.dao.PostImageQueryDao
import com.konkuk.ma.domain.community.domain.image.PostImage
import com.konkuk.ma.domain.community.domain.port.PostImageQueryRepository
import java.time.LocalDateTime
import org.springframework.stereotype.Repository

@Repository
class PostImageQueryCoreRepository(
    private val postImageQueryDao: PostImageQueryDao,
) : PostImageQueryRepository {
    override fun findOneActiveOrNull(postId: Long): PostImage? {
        return postImageQueryDao.findOneActiveOrNull(postId)?.toDomain()
    }

    override fun findActiveByPosts(postIds: List<Long>): List<PostImage> {
        return postImageQueryDao.findActiveByPosts(postIds).map { it.toDomain() }
    }

    override fun findDeletedBefore(cutoff: LocalDateTime, cursorId: Long?, pageSize: Int): List<PostImage> {
        return postImageQueryDao.findDeletedBefore(cutoff, cursorId, pageSize).map { it.toDomain() }
    }
}
