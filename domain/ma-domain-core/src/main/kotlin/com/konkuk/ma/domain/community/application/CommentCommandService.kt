package com.konkuk.ma.domain.community.application

import com.konkuk.ma.domain.community.domain.CommentValidator
import com.konkuk.ma.domain.community.domain.NewComment
import com.konkuk.ma.domain.community.domain.port.CommentCommandRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CommentCommandService(
    private val commentCommandRepository: CommentCommandRepository,
    private val commentValidator: CommentValidator,
) {
    fun create(newComment: NewComment): Long {
        commentValidator.validate(newComment)
        return commentCommandRepository.save(newComment)
    }
}
