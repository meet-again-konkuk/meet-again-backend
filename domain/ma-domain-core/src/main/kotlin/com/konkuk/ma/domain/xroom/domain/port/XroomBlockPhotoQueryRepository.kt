package com.konkuk.ma.domain.xroom.domain.port

import com.konkuk.ma.domain.xroom.domain.block.Photo

interface XroomBlockPhotoQueryRepository {
    fun exists(blockId: Long): Boolean

    fun findOne(photoId: Long): Photo
}
