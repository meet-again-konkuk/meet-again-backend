package com.konkuk.ma.domain.community.domain.image

import com.konkuk.ma.domain.common.domain.file.port.FileStorage
import com.konkuk.ma.domain.community.domain.port.PostImageCommandRepository
import org.springframework.stereotype.Component

@Component
class PostImagePurger(
    private val fileStorage: FileStorage,
    private val postImageCommandRepository: PostImageCommandRepository,
) {
    fun purge(image: PostImage) {
        deleteFiles(image)
        postImageCommandRepository.delete(image.id)
    }

    private fun deleteFiles(image: PostImage) {
        fileStorage.deleteByKey(image.storageKey)
        image.thumbnailKey?.let { fileStorage.deleteByKey(it) }
    }
}
