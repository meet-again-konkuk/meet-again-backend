package com.konkuk.ma.domain.community.api.request

import com.konkuk.ma.domain.community.domain.NewPost
import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.support.validation.ValidationMessages
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

class NewPostRequest(
    val category: PostCategory,

    @field:NotBlank(message = ValidationMessages.POST_TITLE_REQUIRED)
    @field:Size(max = NewPost.MAX_TITLE_LENGTH, message = ValidationMessages.POST_TITLE_SIZE)
    val title: String,

    @field:NotBlank(message = ValidationMessages.POST_CONTENT_REQUIRED)
    @field:Size(max = NewPost.MAX_CONTENT_LENGTH, message = ValidationMessages.POST_CONTENT_SIZE)
    val content: String,
) {
    fun toNewPost(authorId: Long): NewPost {
        return NewPost(
            authorId = authorId,
            category = category,
            title = title,
            content = content,
        )
    }

}
