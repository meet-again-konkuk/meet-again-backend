package com.konkuk.ma.domain.xroom.entity.table

import com.konkuk.ma.domain.common.entity.table.BaseTable

object MemoryMediaTable : BaseTable("MEMORY_MEDIA", "MEDIA_ID") {
    val memoryId = long("MEMORY_ID").index()
    val storageKey = varchar("STORAGE_KEY", 512)
    val originalFilename = varchar("ORIGINAL_FILENAME", 255)
    val mimeType = varchar("MIME_TYPE", 100)
    val fileSize = long("FILE_SIZE")
    val thumbnailKey = varchar("THUMBNAIL_KEY", 512).nullable()
}
