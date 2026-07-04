package com.konkuk.ma.domain.xroom.api.response

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.xroom.domain.media.MediaUploadResult
import com.konkuk.ma.support.id.EncryptId

class MediaUploadResponse(
    @EncryptId(ObfuscationType.MEDIA)
    val mediaId: Long,
    val photoUrl: String,
    val thumbnailUrl: String?,
) {
    companion object {
        fun from(result: MediaUploadResult): MediaUploadResponse {
            return MediaUploadResponse(
                mediaId = result.mediaId,
                photoUrl = result.photoUrl,
                thumbnailUrl = result.thumbnailUrl,
            )
        }
    }
}
