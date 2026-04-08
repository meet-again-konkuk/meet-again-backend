package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.CommentLike
import com.konkuk.ma.domain.community.domain.CommentLikeResult
import com.konkuk.ma.domain.community.domain.port.CommentCommandRepository
import com.konkuk.ma.domain.community.domain.port.CommentLikeRepository
import com.konkuk.ma.domain.community.domain.port.CommentQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CommentLikeService(
    private val commentLikeRepository: CommentLikeRepository,
    private val commentCommandRepository: CommentCommandRepository,
    private val commentQueryRepository: CommentQueryRepository,
) {
    fun toggle(commentId: Long, memberEmail: String): CommentLikeResult {
        val existingLike = commentLikeRepository.findOneOrNull(commentId, memberEmail)
        val liked = existingLike == null

        if (liked) {
            commentLikeRepository.save(CommentLike(commentId = commentId, memberEmail = memberEmail))
            commentCommandRepository.increaseLikes(commentId)
        } else {
            commentLikeRepository.delete(commentId, memberEmail)
            commentCommandRepository.decreaseLikes(commentId)
        }

        val likeCount = commentQueryRepository.findLikeCount(commentId)
        return CommentLikeResult(liked = liked, likeCount = likeCount)
    }
}
