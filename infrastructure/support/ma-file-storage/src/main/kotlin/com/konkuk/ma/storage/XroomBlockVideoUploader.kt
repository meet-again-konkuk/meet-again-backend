package com.konkuk.ma.storage

import com.konkuk.ma.domain.common.domain.file.VideoFile
import com.konkuk.ma.domain.common.domain.file.port.FileStorage
import com.konkuk.ma.domain.xroom.domain.port.VideoUploader
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class XroomBlockVideoUploader(
    private val fileStorage: FileStorage,
) : VideoUploader {
    override fun upload(videoFiles: List<VideoFile>): List<String> {
        return videoFiles.map { upload(it) }
    }

    override fun upload(videoFile: VideoFile): String {
        val storedFileName = "${UUID.randomUUID()}.${videoFile.extension.normalized}"
        return fileStorage.storeBytes(STORAGE_DIRECTORY, storedFileName, videoFile.content)
    }

    companion object {
        private const val STORAGE_DIRECTORY = "xroom/block-video"
    }
}
