package com.konkuk.ma.domain.member.domain.photo

class NewPhoto(
    val memberId: Long,
    val filePath: String,
    val originalFileName: String,
    val thumbnailPath: String? = null
) {
    companion object {
        fun create(
            memberId: Long,
            filePath: String,
            originalFileName: String,
            thumbnailPath: String? = null
        ): NewPhoto {
            return NewPhoto(
                memberId = memberId,
                filePath = filePath,
                originalFileName = originalFileName,
                thumbnailPath = thumbnailPath
            )
        }
    }
}
