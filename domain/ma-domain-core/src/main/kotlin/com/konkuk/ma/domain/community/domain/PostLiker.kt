package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.community.domain.port.PostLikeRepository
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
import org.springframework.stereotype.Component

@Component
class PostLiker(
    private val postLikeRepository: PostLikeRepository,
    private val postQueryRepository: PostQueryRepository,
) {
    fun like(postId: Long, memberId: Long) {
        if (!postQueryRepository.exists(postId)) {
            throw EntityNotFoundException(EntityType.COMMUNITY_POST, postId.toString())
        }
        if (postLikeRepository.exists(postId, memberId)) {
            return
        }
        postLikeRepository.save(PostLike(postId = postId, memberId = memberId))
    }
}
