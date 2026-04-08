package com.konkuk.ma.domain.community.domain

import com.konkuk.ma.domain.community.domain.port.CommentQueryRepository
import org.springframework.stereotype.Component

@Component
class CommentValidator(
    private val commentQueryRepository: CommentQueryRepository,
) {
    fun validate(newComment: NewComment) {
        if (!newComment.hasParent()) return
        val parentComment = commentQueryRepository.findOne(newComment.parentCommentId!!)
        parentComment.validateCanBeParent()
    }
}
