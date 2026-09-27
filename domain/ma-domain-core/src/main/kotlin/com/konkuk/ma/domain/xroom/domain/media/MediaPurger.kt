package com.konkuk.ma.domain.xroom.domain.media

import com.konkuk.ma.domain.common.domain.file.port.FileStorage
import com.konkuk.ma.domain.xroom.domain.port.MediaCommandRepository
import org.springframework.stereotype.Component

@Component
class MediaPurger(
    private val fileStorage: FileStorage,
    private val mediaCommandRepository: MediaCommandRepository,
) {
    fun purge(media: Media) {
        deleteFiles(media)
        mediaCommandRepository.delete(media.id)
    }

    private fun deleteFiles(media: Media) {
        fileStorage.deleteByKey(media.storageKey)
        media.thumbnailKey?.let { fileStorage.deleteByKey(it) }
    }
}
