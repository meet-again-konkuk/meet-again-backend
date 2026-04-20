package com.konkuk.ma.domain.xroom.domain.port

import com.konkuk.ma.domain.xroom.domain.block.NewPhoto

interface XroomBlockPhotoCommandRepository {
    fun saveAll(blockId: Long, newPhotos: List<NewPhoto>): List<Long>

    fun replace(photoId: Long, newPhoto: NewPhoto)
}
