package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.NewComment
import com.konkuk.ma.domain.community.domain.port.CommentCommandRepository
import com.konkuk.ma.domain.community.domain.port.CommentQueryRepository
import com.konkuk.ma.domain.community.domain.port.PostCommandRepository
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import com.konkuk.ma.domain.community.exception.ReplyDepthExceededException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CommentCommandService(
    private val postQueryRepository: PostQueryRepository,
    private val postCommandRepository: PostCommandRepository,
    private val commentCommandRepository: CommentCommandRepository,
    private val commentQueryRepository: CommentQueryRepository,
) {
    fun create(newComment: NewComment): Long {
        postQueryRepository.findOne(newComment.postId)
        validateParentComment(newComment)
        val commentId = commentCommandRepository.save(newComment)
        postCommandRepository.incrementComments(newComment.postId)
        return commentId
    }

    private fun validateParentComment(newComment: NewComment) {
        if (!newComment.hasParent()) return
        val parentComment = commentQueryRepository.findOne(newComment.parentCommentId!!)
        if (parentComment.hasParent()) {
            throw ReplyDepthExceededException(newComment.parentCommentId)
        }
    }
}
