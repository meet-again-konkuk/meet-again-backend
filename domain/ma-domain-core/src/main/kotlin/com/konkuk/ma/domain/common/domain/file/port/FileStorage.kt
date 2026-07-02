package com.konkuk.ma.domain.common.domain.file.port

import com.konkuk.ma.domain.common.domain.file.PhotoFile

interface FileStorage {
    fun store(directory: String, photoFile: PhotoFile): String
    fun storeBytes(directory: String, fileName: String, bytes: ByteArray): String
    fun delete(filePath: String)
    fun deleteByKey(storageKey: String)
}
