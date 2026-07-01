package com.konkuk.ma.domain.xroom.api.response

import com.konkuk.ma.config.WebConfig.Companion.FILE_URL_PREFIX
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
        fun from(media: Media): MediaUploadResponse {
            return MediaUploadResponse(
                mediaId = media.id,
                photoUrl = media.toPhotoUrl(FILE_URL_PREFIX),
                thumbnailUrl = media.toThumbnailUrl(FILE_URL_PREFIX),
            )
        }
    }
}
