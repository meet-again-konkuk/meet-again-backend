package com.konkuk.ma.domain.community.repository

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
}
