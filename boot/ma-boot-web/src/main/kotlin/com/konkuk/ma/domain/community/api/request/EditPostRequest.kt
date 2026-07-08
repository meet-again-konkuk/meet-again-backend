package com.konkuk.ma.domain.community.api.request

import com.konkuk.ma.domain.community.domain.PostCategory
import com.konkuk.ma.domain.community.domain.PostDetails
import com.konkuk.ma.support.validation.ValidationMessages
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

class EditPostRequest(
    val category: PostCategory,

    @field:NotBlank(message = ValidationMessages.POST_TITLE_REQUIRED)
    @field:Size(max = PostDetails.MAX_TITLE_LENGTH, message = ValidationMessages.POST_TITLE_SIZE)
    val title: String,

    @field:NotBlank(message = ValidationMessages.POST_CONTENT_REQUIRED)
    @field:Size(max = PostDetails.MAX_CONTENT_LENGTH, message = ValidationMessages.POST_CONTENT_SIZE)
    val content: String,
) {
    fun toPostDetails(): PostDetails {
        return PostDetails(
            category = category,
            title = title,
            content = content,
        )
    }
}
