package com.konkuk.ma.domain.common.fixture

import com.konkuk.ma.domain.common.domain.file.AllowedVideoExtension
import com.konkuk.ma.domain.common.domain.file.VideoFile

object VideoFileFixture {
    fun create(
        originalFileName: String = "video.mp4",
        extension: AllowedVideoExtension = AllowedVideoExtension.from(originalFileName),
        sizeInBytes: Long = 1024L,
        content: ByteArray = ByteArray(1024) { 1 },
    ): VideoFile {
        return VideoFile(
            originalFileName = originalFileName,
            extension = extension,
            sizeInBytes = sizeInBytes,
            content = content,
        )
    }
}
