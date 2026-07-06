package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.community.domain.port.PostLikeRepository
import org.springframework.stereotype.Component

@Component
class PostLiker(
    private val postLikeRepository: PostLikeRepository,
) {
    fun like(postId: Long, memberId: Long) {
        if (postLikeRepository.exists(postId, memberId)) {
            return
        }
        postLikeRepository.save(PostLike(postId = postId, memberId = memberId))
    }
}
