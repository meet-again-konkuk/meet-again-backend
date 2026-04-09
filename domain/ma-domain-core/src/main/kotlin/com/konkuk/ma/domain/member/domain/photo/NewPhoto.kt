package com.konkuk.ma.domain.member.domain.photo

import com.konkuk.ma.domain.common.domain.Email

class NewPhoto(
    val memberEmail: Email,
    val filePath: String,
    val originalFileName: String,
    val thumbnailPath: String? = null
) {
    companion object {
        fun create(
            memberEmail: Email,
            filePath: String,
            originalFileName: String,
            thumbnailPath: String? = null
        ): NewPhoto {
            return NewPhoto(
                memberEmail = memberEmail,
                filePath = filePath,
                originalFileName = originalFileName,
                thumbnailPath = thumbnailPath
            )
        }
    }
}
