package com.konkuk.ma.domain.xroom.domain.port

import com.konkuk.ma.domain.common.domain.file.PhotoFile

interface PhotoUploader {
    fun upload(photoFiles: List<PhotoFile>): List<String>

    fun upload(photoFile: PhotoFile): String
}
