package com.konkuk.ma.domain.support.api.request

import com.konkuk.ma.domain.support.domain.NewInquiry
import com.konkuk.ma.support.validation.ValidationMessages
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

class NewInquiryRequest(
    @field:NotBlank(message = ValidationMessages.INQUIRY_TITLE_REQUIRED)
    @field:Size(max = NewInquiry.MAX_TITLE_LENGTH, message = ValidationMessages.INQUIRY_TITLE_SIZE)
    val title: String,

    @field:NotBlank(message = ValidationMessages.INQUIRY_CONTENT_REQUIRED)
    @field:Size(max = NewInquiry.MAX_CONTENT_LENGTH, message = ValidationMessages.INQUIRY_CONTENT_SIZE)
    val content: String,
) {
    fun toNewInquiry(authorEmail: String): NewInquiry {
        return NewInquiry(
            authorEmail = authorEmail,
            title = title,
            content = content,
        )
    }
}
