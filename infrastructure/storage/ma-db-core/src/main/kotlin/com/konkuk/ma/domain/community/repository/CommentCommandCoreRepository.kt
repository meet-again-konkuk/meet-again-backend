package com.konkuk.ma.domain.community.repository

import com.konkuk.ma.domain.community.dao.CommentCommandDao
import com.konkuk.ma.domain.community.domain.NewComment
import com.konkuk.ma.domain.community.domain.port.CommentCommandRepository
import org.springframework.stereotype.Repository

@Repository
class CommentCommandCoreRepository(
    private val commentCommandDao: CommentCommandDao,
) : CommentCommandRepository {
    override fun save(newComment: NewComment): Long {
        return commentCommandDao.save(newComment)
    }

    override fun increaseLikes(commentId: Long) {
        commentCommandDao.increaseLikes(commentId)
    }

    override fun decreaseLikes(commentId: Long) {
        commentCommandDao.decreaseLikes(commentId)
    }
}
