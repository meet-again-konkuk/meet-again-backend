package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.common.domain.Email
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
    fun like(commentId: Long, memberEmail: String): CommentLikeResult {
        commentLikeRepository.save(CommentLike(commentId = commentId, memberEmail = Email(memberEmail)))
        return CommentLikeResult.liked(commentLikeRepository.count(commentId))
    }

    fun unlike(commentId: Long, memberEmail: String): CommentLikeResult {
        commentLikeRepository.delete(commentId, Email(memberEmail))
        return CommentLikeResult.unliked(commentLikeRepository.count(commentId))
    }
}
