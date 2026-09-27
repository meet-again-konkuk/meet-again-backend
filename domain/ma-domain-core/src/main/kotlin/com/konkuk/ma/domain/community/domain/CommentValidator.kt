package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.community.domain.port.CommentQueryRepository
import com.konkuk.ma.domain.community.domain.port.PostQueryRepository
import com.konkuk.ma.exception.EntityNotFoundException
import com.konkuk.ma.exception.EntityType
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
            throw EntityNotFoundException(EntityType.COMMUNITY_POST, postId.toString())
        }
    }

    private fun validateParentComment(newComment: NewComment) {
        if (!newComment.hasParent()) return
        val parentComment = commentQueryRepository.findOne(newComment.parentCommentId!!)
        parentComment.validateIsRootComment()
    }
}
