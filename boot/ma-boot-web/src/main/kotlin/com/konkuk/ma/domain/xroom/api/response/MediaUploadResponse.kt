package com.konkuk.ma.domain.xroom.api.response

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.xroom.domain.media.Media
import com.konkuk.ma.support.id.EncryptId

class MediaUploadResponse(
    @EncryptId(ObfuscationType.MEDIA)
    val mediaId: Long,
    val photoUrl: String,
    val thumbnailUrl: String?,
) {
    companion object {
        fun from(media: Media, baseUrl: String): MediaUploadResponse {
            return MediaUploadResponse(
                mediaId = media.id,
                photoUrl = media.toPhotoUrl(baseUrl),
                thumbnailUrl = media.toThumbnailUrl(baseUrl),
            )
        }
    }
}
