package com.konkuk.ma.domain.member.domain.photo

class NewPhoto(
    val memberEmail: String,
    val filePath: String,
    val originalFileName: String
) {
    companion object {
        fun create(
            memberEmail: String,
            filePath: String,
            originalFileName: String
        ): NewPhoto {
            return NewPhoto(
                memberEmail = memberEmail,
                filePath = filePath,
                originalFileName = originalFileName
            )
        }
    }
}
