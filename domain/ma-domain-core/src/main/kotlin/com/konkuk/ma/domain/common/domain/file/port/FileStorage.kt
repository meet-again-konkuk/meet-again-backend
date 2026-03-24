package com.konkuk.ma.domain.common.domain.file.port

import com.konkuk.ma.domain.common.domain.file.PhotoFile

interface FileStorage {
    fun store(directory: String, photoFile: PhotoFile): String
    fun delete(filePath: String)
}
