package com.konkuk.ma.domain.community.repository

import com.konkuk.ma.domain.community.dao.PostLikeDao
import com.konkuk.ma.domain.community.domain.PostLike
import com.konkuk.ma.domain.community.domain.port.PostLikeRepository
import org.springframework.stereotype.Repository

@Repository
class PostLikeCoreRepository(
    private val postLikeDao: PostLikeDao,
) : PostLikeRepository {
    override fun exists(postId: Long, memberId: Long): Boolean {
        return postLikeDao.exists(postId, memberId)
    }

    override fun save(postLike: PostLike) {
        postLikeDao.save(postLike.postId, postLike.memberId)
    }

    override fun find(memberId: Long): List<PostLike> {
        return postLikeDao.find(memberId).map { it.toDomain() }
    }

    override fun count(postId: Long): Int {
        return postLikeDao.count(postId)
    }

    override fun count(postIds: List<Long>): Map<Long, Int> {
        return postLikeDao.count(postIds)
    }

    override fun findLikedPostIds(memberId: Long, postIds: List<Long>): Set<Long> {
        return postLikeDao.findLikedPostIds(memberId, postIds)
    }

    override fun delete(postId: Long, memberId: Long) {
        postLikeDao.delete(postId, memberId)
    }

    override fun deleteByMember(memberId: Long) {
        postLikeDao.deleteByMember(memberId)
    }
}
