package com.konkuk.ma.domain.member.domain.photo

class NewPhoto(
    val memberId: Long,
    val storageKey: String,
    val originalFileName: String,
    val thumbnailKey: String? = null
) {
    companion object {
        fun create(
            memberId: Long,
            storageKey: String,
            originalFileName: String,
            thumbnailKey: String? = null
        ): NewPhoto {
            return NewPhoto(
                memberId = memberId,
                storageKey = storageKey,
                originalFileName = originalFileName,
                thumbnailKey = thumbnailKey
            )
        }
    }
}
