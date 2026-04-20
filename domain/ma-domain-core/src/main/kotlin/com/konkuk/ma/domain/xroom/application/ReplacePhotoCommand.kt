package com.konkuk.ma.domain.xroom.application

import com.konkuk.ma.domain.common.domain.file.PhotoFile

class ReplacePhotoCommand(
    val blockId: Long,
    val photoId: Long,
    val email: String,
    val photoFile: PhotoFile,
)
