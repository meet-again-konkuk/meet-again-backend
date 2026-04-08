package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.CommentLike
import com.konkuk.ma.domain.community.domain.CommentLikeResult
import com.konkuk.ma.domain.community.domain.port.CommentCommandRepository
import com.konkuk.ma.domain.community.domain.port.CommentLikeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CommentLikeService(
    private val commentLikeRepository: CommentLikeRepository,
    private val commentCommandRepository: CommentCommandRepository,
) {
    fun like(commentId: Long, memberEmail: String): CommentLikeResult {
        commentLikeRepository.save(CommentLike(commentId = commentId, memberEmail = memberEmail))
        val likeCount = commentCommandRepository.increaseLikes(commentId)
        return CommentLikeResult.liked(likeCount)
    }

    fun unlike(commentId: Long, memberEmail: String): CommentLikeResult {
        commentLikeRepository.delete(commentId, memberEmail)
        val likeCount = commentCommandRepository.decreaseLikes(commentId)
        return CommentLikeResult.unliked(likeCount)
    }
}
