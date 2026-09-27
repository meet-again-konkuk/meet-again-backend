package com.konkuk.ma.domain.xroom.domain.media

import com.konkuk.ma.domain.common.domain.file.FileUrls
import com.konkuk.ma.domain.common.domain.file.port.FileUrlResolver
import org.springframework.stereotype.Component

@Component
class MediaUrlResolver(
    private val fileUrlResolver: FileUrlResolver,
) {
    fun resolve(media: Media): MediaUrls {
        return MediaUrls(
            photoUrl = fileUrlResolver.resolve(media.storageKey),
            thumbnailUrl = media.thumbnailKey?.let { fileUrlResolver.resolve(it) },
        )
    }

    fun resolveByMemory(medias: List<Media>): FileUrls {
        return FileUrls(medias.associate { it.memoryId to fileUrlResolver.resolve(it.storageKey) })
    }
}
