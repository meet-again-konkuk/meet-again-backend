package com.konkuk.ma.domain.community.repository

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.community.dao.CommentQueryDao
import com.konkuk.ma.domain.community.domain.Comment
import com.konkuk.ma.domain.community.domain.port.CommentQueryRepository
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Repository

@Repository
class CommentQueryCoreRepository(
    private val commentQueryDao: CommentQueryDao,
) : CommentQueryRepository {
    override fun findOne(id: Long): Comment {
        return commentQueryDao.findOne(id)?.toDomain()
            ?: throw EntityNotFoundException(EntityType.COMMUNITY_COMMENT, id.toString())
    }

    override fun find(postId: Long): List<Comment> {
        return commentQueryDao.find(postId).map { it.toDomain() }
    }

    override fun find(authorEmail: Email): List<Comment> {
        return commentQueryDao.find(authorEmail.value).map { it.toDomain() }
    }

    override fun findReplies(parentCommentId: Long): List<Comment> {
        return commentQueryDao.findReplies(parentCommentId).map { it.toDomain() }
    }
}
