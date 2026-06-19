package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.CommentLike
import com.konkuk.ma.domain.community.domain.CommentLikeResult
import com.konkuk.ma.domain.community.domain.port.CommentLikeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CommentLikeService(
    private val commentLikeRepository: CommentLikeRepository,
) {
    fun like(commentId: Long, memberId: Long): CommentLikeResult {
        commentLikeRepository.save(CommentLike(commentId = commentId, memberId = memberId))
        return CommentLikeResult.liked(commentLikeRepository.count(commentId))
    }

    fun unlike(commentId: Long, memberId: Long): CommentLikeResult {
        commentLikeRepository.delete(commentId, memberId)
        return CommentLikeResult.unliked(commentLikeRepository.count(commentId))
    }
}
