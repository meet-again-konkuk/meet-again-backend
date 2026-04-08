package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.community.domain.port.CommentQueryRepository
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import com.konkuk.ma.domain.community.exception.PostNotFoundException
import org.springframework.stereotype.Component

@Component
class CommentValidator(
    private val postQueryRepository: PostQueryRepository,
    private val commentQueryRepository: CommentQueryRepository,
) {
    fun validate(newComment: NewComment) {
        validatePostExists(newComment.postId)
        validateParentComment(newComment)
    }

    private fun validatePostExists(postId: Long) {
        if (!postQueryRepository.exists(postId)) {
            throw PostNotFoundException(postId)
        }
    }

    private fun validateParentComment(newComment: NewComment) {
        if (!newComment.hasParent()) return
        val parentComment = commentQueryRepository.findOne(newComment.parentCommentId!!)
        parentComment.validateCanBeParent()
    }
}
