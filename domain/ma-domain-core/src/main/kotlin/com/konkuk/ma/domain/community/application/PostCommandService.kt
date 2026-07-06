package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.NewPost
import com.konkuk.ma.domain.community.domain.PostDetails
import com.konkuk.ma.domain.community.domain.port.PostCommandRepository
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PostCommandService(
    private val postCommandRepository: PostCommandRepository,
    private val postQueryRepository: PostQueryRepository,
) {
    fun create(newPost: NewPost): Long {
        return postCommandRepository.save(newPost)
    }

    fun update(postId: Long, memberId: Long, details: PostDetails) {
        val post = postQueryRepository.findOne(postId)
        post.validateOwnership(memberId)
        postCommandRepository.update(postId, details)
    }

    fun delete(postId: Long, memberId: Long) {
        val post = postQueryRepository.findOne(postId)
        post.validateOwnership(memberId)
        postCommandRepository.softDelete(postId, memberId)
    }
}
