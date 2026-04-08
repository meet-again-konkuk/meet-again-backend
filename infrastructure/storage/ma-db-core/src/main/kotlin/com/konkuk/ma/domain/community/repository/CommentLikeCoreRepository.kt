package com.konkuk.ma.domain.community.repository

import com.konkuk.ma.domain.community.dao.CommentLikeDao
import com.konkuk.ma.domain.community.domain.CommentLike
import com.konkuk.ma.domain.community.domain.port.CommentLikeRepository
import org.springframework.stereotype.Repository

@Repository
class CommentLikeCoreRepository(
    private val commentLikeDao: CommentLikeDao,
) : CommentLikeRepository {
    override fun save(commentLike: CommentLike): Long {
        return commentLikeDao.save(commentLike)
    }

    override fun delete(commentId: Long, memberEmail: String) {
        commentLikeDao.delete(commentId, memberEmail)
    }
}
