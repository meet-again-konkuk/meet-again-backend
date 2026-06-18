package com.konkuk.ma.domain.community.repository

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.community.dao.PostLikeDao
import com.konkuk.ma.domain.community.domain.PostLike
import com.konkuk.ma.domain.community.domain.port.PostLikeRepository
import org.springframework.stereotype.Repository

@Repository
class PostLikeCoreRepository(
    private val postLikeDao: PostLikeDao,
) : PostLikeRepository {
    override fun save(postLike: PostLike): Long {
        return postLikeDao.save(postLike.postId, postLike.memberEmail.value)
    }

    override fun find(memberEmail: Email): List<PostLike> {
        return postLikeDao.find(memberEmail.value).map { it.toDomain() }
    }

    override fun count(postId: Long): Int {
        return postLikeDao.count(postId)
    }

    override fun count(postIds: List<Long>): Map<Long, Int> {
        return postLikeDao.count(postIds)
    }

    override fun delete(postId: Long, memberEmail: Email) {
        postLikeDao.delete(postId, memberEmail.value)
    }

    override fun deleteByMember(memberEmail: Email) {
        postLikeDao.deleteByMember(memberEmail.value)
    }
}
