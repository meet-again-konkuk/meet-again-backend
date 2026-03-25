package com.konkuk.ma.domain.common.fixture

import com.konkuk.ma.domain.common.domain.file.AllowedExtension
import com.konkuk.ma.domain.common.domain.file.PhotoFile

object PhotoFileFixture {
    fun create(
        originalFileName: String = "photo.jpg",
        extension: AllowedExtension = AllowedExtension.from(originalFileName),
        sizeInBytes: Long = 1024L,
        content: ByteArray = ByteArray(1024) { 1 }
    ): PhotoFile {
        return PhotoFile(
            originalFileName = originalFileName,
            extension = extension,
            sizeInBytes = sizeInBytes,
            content = content
        )
    }
}
