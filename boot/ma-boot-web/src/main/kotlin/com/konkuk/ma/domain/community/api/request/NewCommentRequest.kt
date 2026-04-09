package com.konkuk.ma.domain.community.api.request

import com.konkuk.ma.domain.common.domain.Email
import com.konkuk.ma.domain.community.domain.NewComment
import com.konkuk.ma.support.validation.ValidationMessages
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

class NewCommentRequest(
    @field:NotBlank(message = ValidationMessages.COMMENT_CONTENT_REQUIRED)
    @field:Size(max = NewComment.MAX_CONTENT_LENGTH, message = ValidationMessages.COMMENT_CONTENT_SIZE)
    val content: String,

    val parentCommentId: Long? = null,
) {
    fun toNewComment(authorEmail: String, postId: Long): NewComment {
        return NewComment(
            postId = postId,
            authorEmail = Email(authorEmail),
            content = content,
            parentCommentId = parentCommentId,
        )
    }

}
