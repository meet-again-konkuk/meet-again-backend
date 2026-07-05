package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.community.domain.port.CommentLikeRepository
import org.springframework.stereotype.Component

@Component
class CommentLiker(
    private val commentLikeRepository: CommentLikeRepository,
) {
    fun like(commentId: Long, memberId: Long) {
        if (commentLikeRepository.exists(commentId, memberId)) {
            return
        }
        commentLikeRepository.save(CommentLike(commentId = commentId, memberId = memberId))
    }
}
