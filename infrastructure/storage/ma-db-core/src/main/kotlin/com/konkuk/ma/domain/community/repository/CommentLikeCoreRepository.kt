package com.konkuk.ma.domain.community.repository

import com.konkuk.ma.domain.community.dao.CommentLikeDao
import com.konkuk.ma.domain.community.domain.CommentLike
import com.konkuk.ma.domain.community.domain.port.CommentLikeRepository
import org.springframework.stereotype.Repository

@Repository
class CommentLikeCoreRepository(
    private val commentLikeDao: CommentLikeDao,
) : CommentLikeRepository {
    override fun save(commentLike: CommentLike) {
        commentLikeDao.save(commentLike.commentId, commentLike.memberId)
    }

    override fun find(memberId: Long): List<CommentLike> {
        return commentLikeDao.find(memberId).map { it.toDomain() }
    }

    override fun count(commentId: Long): Int {
        return commentLikeDao.count(commentId)
    }

    override fun count(commentIds: List<Long>): Map<Long, Int> {
        return commentLikeDao.count(commentIds)
    }

    override fun delete(commentId: Long, memberId: Long) {
        commentLikeDao.delete(commentId, memberId)
    }

    override fun deleteByMember(memberId: Long) {
        commentLikeDao.deleteByMember(memberId)
    }
}
