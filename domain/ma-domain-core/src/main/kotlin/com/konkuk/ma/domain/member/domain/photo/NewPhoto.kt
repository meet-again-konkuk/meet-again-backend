package com.konkuk.ma.domain.member.domain.photo

class NewPhoto(
    val memberEmail: String,
    val filePath: String,
    val originalFileName: String,
    val thumbnailPath: String? = null
) {
    companion object {
        fun create(
            memberEmail: String,
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
