package com.konkuk.ma.storage

import com.konkuk.ma.domain.common.domain.file.PhotoFile
import com.konkuk.ma.domain.common.domain.file.port.FileStorage
import com.konkuk.ma.domain.xroom.domain.port.PhotoUploader
import org.springframework.stereotype.Component

@Component
class XroomBlockPhotoUploader(
    private val fileStorage: FileStorage,
) : PhotoUploader {
    override fun upload(photoFiles: List<PhotoFile>): List<String> {
        return photoFiles.map { upload(it) }
    }

    override fun upload(photoFile: PhotoFile): String {
        return fileStorage.store(STORAGE_DIRECTORY, photoFile)
    }

    companion object {
        private const val STORAGE_DIRECTORY = "xroom/block-photo"
    }
}
