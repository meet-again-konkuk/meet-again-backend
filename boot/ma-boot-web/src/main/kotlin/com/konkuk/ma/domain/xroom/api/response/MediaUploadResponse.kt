package com.konkuk.ma.domain.xroom.api.response

import com.konkuk.ma.domain.common.domain.id.ObfuscationType
import com.konkuk.ma.domain.xroom.api.MediaUrlAssembler
import com.konkuk.ma.domain.xroom.application.result.MediaUploadResult
import com.konkuk.ma.support.id.EncryptId

class MediaUploadResponse(
    @EncryptId(ObfuscationType.MEDIA)
    val mediaId: Long,
    val photoUrl: String,
    val thumbnailUrl: String?,
) {
    companion object {
        fun from(result: MediaUploadResult, mediaUrlAssembler: MediaUrlAssembler): MediaUploadResponse {
            return MediaUploadResponse(
                mediaId = result.mediaId,
                photoUrl = mediaUrlAssembler.toUrl(result.storageKey),
                thumbnailUrl = mediaUrlAssembler.toUrlOrNull(result.thumbnailKey),
            )
        }
    }
}
