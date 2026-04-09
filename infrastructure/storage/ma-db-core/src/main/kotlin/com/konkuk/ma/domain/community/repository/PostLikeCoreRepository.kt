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

    override fun delete(postId: Long, memberEmail: Email) {
        postLikeDao.delete(postId, memberEmail.value)
    }
}
