package com.konkuk.ma.domain.community.repository

import com.konkuk.ma.domain.community.dao.PostImageCommandDao
import com.konkuk.ma.domain.community.domain.image.NewPostImage
import com.konkuk.ma.domain.community.domain.port.PostImageCommandRepository
import org.springframework.stereotype.Repository

@Repository
class PostImageCommandCoreRepository(
    private val postImageCommandDao: PostImageCommandDao,
) : PostImageCommandRepository {
    override fun save(newPostImage: NewPostImage): Long {
        return postImageCommandDao.save(newPostImage)
    }

    override fun softDeleteByPost(postId: Long, memberId: Long) {
        postImageCommandDao.softDeleteByPost(postId, memberId)
    }

    override fun delete(imageId: Long) {
        postImageCommandDao.delete(imageId)
    }
}
