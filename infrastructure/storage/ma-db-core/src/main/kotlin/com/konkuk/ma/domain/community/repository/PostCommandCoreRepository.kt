package com.konkuk.ma.domain.community.repository

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.community.dao.PostCommandDao
import com.konkuk.ma.domain.community.domain.NewPost
import com.konkuk.ma.domain.community.domain.port.PostCommandRepository
import org.springframework.stereotype.Repository

@Repository
class PostCommandCoreRepository(
    private val postCommandDao: PostCommandDao,
) : PostCommandRepository {
    override fun save(newPost: NewPost): Long {
        return postCommandDao.save(newPost)
    }

    override fun increaseLikes(postId: Long): Int {
        return postCommandDao.increaseLikes(postId)
    }

    override fun decreaseLikes(postId: Long): Int {
        return postCommandDao.decreaseLikes(postId)
    }

    override fun anonymizeAuthor(authorEmail: Email, withdrawnEmail: Email) {
        postCommandDao.anonymizeAuthor(authorEmail.value, withdrawnEmail.value)
    }
}
